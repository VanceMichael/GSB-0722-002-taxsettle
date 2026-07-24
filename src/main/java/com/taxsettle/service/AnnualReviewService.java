package com.taxsettle.service;

import com.taxsettle.entity.*;
import com.taxsettle.entity.enums.RiskLevel;
import com.taxsettle.entity.enums.RiskType;
import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnualReviewService {
    private final AnnualReviewRepository annualReviewRepository;
    private final ReviewRiskRepository reviewRiskRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final SpecialDeductionRepository specialDeductionRepository;
    private final TaxSettlementRepository taxSettlementRepository;
    private final EmployerRepository employerRepository;

    @Transactional
    public AnnualReview performReview(Long taxpayerId, Integer taxYear) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));

        AnnualReview review = annualReviewRepository.findByTaxpayerIdAndTaxYear(taxpayerId, taxYear)
                .orElse(AnnualReview.builder()
                        .taxpayer(taxpayer)
                        .taxYear(taxYear)
                        .build());

        reviewRiskRepository.deleteByAnnualReviewId(review.getId());
        review.setRisks(new ArrayList<>());
        review.setReviewed(false);
        review.setReviewedAt(null);
        review.setReviewConclusion(null);
        review.setReviewerRemark(null);

        List<ReviewRisk> risks = new ArrayList<>();

        List<IncomeRecord> incomes = incomeRecordRepository.findByTaxpayerId(taxpayerId);
        List<SpecialDeduction> deductions = specialDeductionRepository.findByTaxpayerId(taxpayerId);
        Optional<TaxSettlement> settlementOpt = taxSettlementRepository.findByTaxpayerIdAndTaxYear(taxpayerId, taxYear);

        analyzeMultipleEmployerIncome(taxpayer, incomes, risks, review);
        analyzeIncomeFluctuation(incomes, risks, review);
        analyzeDuplicateDeductionTargets(taxpayer, deductions, risks, review);
        analyzePrepaidTaxSufficiency(taxpayer, settlementOpt.orElse(null), incomes, risks, review);
        analyzeDeductionExceedLimit(deductions, risks);

        RiskLevel overallLevel = determineOverallRiskLevel(risks);
        review.setOverallRiskLevel(overallLevel);
        review.setRisks(risks);

        if (overallLevel != RiskLevel.NONE) {
            taxpayer.setStatus(SettlementStatus.UNDER_REVIEW);
            taxpayerRepository.save(taxpayer);
        }

        AnnualReview saved = annualReviewRepository.save(review);

        for (ReviewRisk risk : risks) {
            risk.setAnnualReview(saved);
            reviewRiskRepository.save(risk);
        }
        saved.setRisks(risks);

        return saved;
    }

    private void analyzeMultipleEmployerIncome(Taxpayer taxpayer, List<IncomeRecord> incomes,
                                                List<ReviewRisk> risks, AnnualReview review) {
        Set<Long> employerIds = new HashSet<>();
        Employer primaryEmployer = taxpayer.getEmployer();
        if (primaryEmployer != null) {
            employerIds.add(primaryEmployer.getId());
        }
        for (IncomeRecord inc : incomes) {
            if (inc.getEmployer() != null) {
                employerIds.add(inc.getEmployer().getId());
            }
        }

        review.setEmployerCount(employerIds.size());

        BigDecimal totalAllEmployers = incomes.stream()
                .map(IncomeRecord::getGrossIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        review.setTotalIncomeFromAllEmployers(totalAllEmployers);

        if (employerIds.size() > 1) {
            StringBuilder detail = new StringBuilder();
            for (Long eid : employerIds) {
                employerRepository.findById(eid).ifPresent(e -> {
                    BigDecimal empTotal = incomes.stream()
                            .filter(i -> i.getEmployer() != null && i.getEmployer().getId().equals(eid))
                            .map(IncomeRecord::getGrossIncome)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    detail.append(e.getName()).append(": ").append(empTotal).append("元; ");
                });
            }
            risks.add(ReviewRisk.builder()
                    .riskType(RiskType.MULTIPLE_EMPLOYER_INCOME)
                    .riskLevel(employerIds.size() >= 3 ? RiskLevel.HIGH : RiskLevel.MEDIUM)
                    .description("该纳税人从" + employerIds.size() + "个任职单位取得收入")
                    .relatedAmount(totalAllEmployers)
                    .detail(detail.toString())
                    .build());
        }
    }

    private void analyzeIncomeFluctuation(List<IncomeRecord> incomes, List<ReviewRisk> risks, AnnualReview review) {
        if (incomes.isEmpty()) return;

        Map<Integer, BigDecimal> monthlyIncome = new TreeMap<>();
        for (IncomeRecord inc : incomes) {
            monthlyIncome.merge(inc.getMonth(), inc.getGrossIncome(), BigDecimal::add);
        }

        if (monthlyIncome.size() < 2) return;

        List<BigDecimal> values = new ArrayList<>(monthlyIncome.values());
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);

        if (avg.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal maxDeviation = BigDecimal.ZERO;
        int maxMonth = -1;
        for (Map.Entry<Integer, BigDecimal> entry : monthlyIncome.entrySet()) {
            BigDecimal deviation = entry.getValue().subtract(avg).abs()
                    .divide(avg, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (deviation.compareTo(maxDeviation) > 0) {
                maxDeviation = deviation;
                maxMonth = entry.getKey();
            }
        }

        review.setIncomeFluctuationRate(maxDeviation);

        if (maxDeviation.compareTo(new BigDecimal("50")) > 0) {
            RiskLevel level = maxDeviation.compareTo(new BigDecimal("100")) > 0 ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            risks.add(ReviewRisk.builder()
                    .riskType(RiskType.INCOME_FLUCTUATION)
                    .riskLevel(level)
                    .description(maxMonth + "月收入与月均收入偏离" + maxDeviation.setScale(2, RoundingMode.HALF_UP) + "%")
                    .relatedAmount(monthlyIncome.get(maxMonth))
                    .detail("月均收入: " + avg + "元, " + maxMonth + "月收入: " + monthlyIncome.get(maxMonth) + "元")
                    .build());
        }
    }

    private void analyzeDuplicateDeductionTargets(Taxpayer taxpayer, List<SpecialDeduction> deductions,
                                                   List<ReviewRisk> risks, AnnualReview review) {
        int duplicateCount = 0;
        Set<String> processedMembers = new HashSet<>();

        for (SpecialDeduction ded : deductions) {
            if (ded.getFamilyMember() == null) continue;
            String memberId = ded.getFamilyMember().getIdNumber();
            if (processedMembers.contains(memberId)) continue;
            processedMembers.add(memberId);

            List<FamilyMember> duplicates = familyMemberRepository
                    .findByIdNumberAndTaxpayerIdNot(memberId, taxpayer.getId());

            if (!duplicates.isEmpty()) {
                duplicateCount++;
                Set<String> otherTaxpayers = duplicates.stream()
                        .map(fm -> fm.getTaxpayer().getName())
                        .collect(Collectors.toSet());

                risks.add(ReviewRisk.builder()
                        .riskType(RiskType.DEDUCTION_TARGET_DUPLICATE)
                        .riskLevel(RiskLevel.HIGH)
                        .description("家庭成员[" + ded.getFamilyMember().getName() + "]同时被" + (otherTaxpayers.size() + 1) + "人申报扣除")
                        .relatedAmount(ded.getAnnualAmount())
                        .detail("其他申报人: " + String.join(", ", otherTaxpayers))
                        .build());
            }
        }
        review.setDuplicateDeductionCount(duplicateCount);
    }

    private void analyzePrepaidTaxSufficiency(Taxpayer taxpayer, TaxSettlement settlement,
                                               List<IncomeRecord> incomes, List<ReviewRisk> risks,
                                               AnnualReview review) {
        BigDecimal totalPrepaid = incomes.stream()
                .map(IncomeRecord::getPrepaidTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (settlement != null) {
            BigDecimal annualPayable = settlement.getAnnualTaxPayable();
            if (annualPayable.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal coverage = totalPrepaid.divide(annualPayable, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                review.setPrepaidTaxCoverageRate(coverage);

                if (coverage.compareTo(new BigDecimal("60")) < 0
                        && settlement.getDifference().compareTo(BigDecimal.ZERO) > 0) {
                    RiskLevel level;
                    if (coverage.compareTo(new BigDecimal("30")) < 0) {
                        level = RiskLevel.HIGH;
                    } else {
                        level = RiskLevel.MEDIUM;
                    }
                    risks.add(ReviewRisk.builder()
                            .riskType(RiskType.PREPAID_TAX_INSUFFICIENT)
                            .riskLevel(level)
                            .description("预缴税款覆盖率仅" + coverage.setScale(2, RoundingMode.HALF_UP) + "%，存在明显不足")
                            .relatedAmount(settlement.getDifference())
                            .detail("应缴税款: " + annualPayable + "元, 已预缴: " + totalPrepaid + "元, 需补缴: " + settlement.getDifference() + "元")
                            .build());
                }
            }
        }
    }

    private void analyzeDeductionExceedLimit(List<SpecialDeduction> deductions, List<ReviewRisk> risks) {
        for (SpecialDeduction ded : deductions) {
            if (ded.getAnnualAmount().compareTo(ded.getAnnualLimit()) > 0) {
                BigDecimal excess = ded.getAnnualAmount().subtract(ded.getAnnualLimit());
                risks.add(ReviewRisk.builder()
                        .riskType(RiskType.DEDUCTION_EXCEED_LIMIT)
                        .riskLevel(RiskLevel.MEDIUM)
                        .description("[" + ded.getDeductionType() + "]专项附加扣除超出限额")
                        .relatedAmount(excess)
                        .detail("申报金额: " + ded.getAnnualAmount() + "元, 限额: " + ded.getAnnualLimit() + "元, 超限额: " + excess + "元")
                        .build());
            }
        }
    }

    private RiskLevel determineOverallRiskLevel(List<ReviewRisk> risks) {
        if (risks.isEmpty()) return RiskLevel.NONE;
        boolean hasHigh = risks.stream().anyMatch(r -> r.getRiskLevel() == RiskLevel.HIGH);
        if (hasHigh) return RiskLevel.HIGH;
        boolean hasMedium = risks.stream().anyMatch(r -> r.getRiskLevel() == RiskLevel.MEDIUM);
        if (hasMedium) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public AnnualReview findByTaxpayerIdAndTaxYear(Long taxpayerId, Integer taxYear) {
        return annualReviewRepository.findByTaxpayerIdAndTaxYear(taxpayerId, taxYear)
                .orElseThrow(() -> new RuntimeException("Review not found for taxpayer: " + taxpayerId + ", year: " + taxYear));
    }

    public List<AnnualReview> findByTaxYear(Integer taxYear) {
        return annualReviewRepository.findByTaxYear(taxYear);
    }

    public List<AnnualReview> findByRiskLevelAndTaxYear(RiskLevel riskLevel, Integer taxYear) {
        return annualReviewRepository.findByOverallRiskLevelAndTaxYear(riskLevel, taxYear);
    }

    public List<AnnualReview> findByEmployerIdAndTaxYear(Long employerId, Integer taxYear) {
        return annualReviewRepository.findByEmployerIdAndTaxYear(employerId, taxYear);
    }

    @Transactional
    public AnnualReview concludeReview(Long reviewId, SettlementStatus conclusion, String remark) {
        AnnualReview review = annualReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

        if (conclusion != SettlementStatus.REVIEW_PASSED && conclusion != SettlementStatus.REVIEW_FAILED) {
            throw new IllegalArgumentException("Conclusion must be REVIEW_PASSED or REVIEW_FAILED");
        }

        review.setReviewConclusion(conclusion);
        review.setReviewerRemark(remark);
        review.setReviewed(true);
        review.setReviewedAt(LocalDateTime.now());

        Taxpayer taxpayer = review.getTaxpayer();
        taxpayer.setStatus(conclusion);
        taxpayerRepository.save(taxpayer);

        return annualReviewRepository.save(review);
    }
}
