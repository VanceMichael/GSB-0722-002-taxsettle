package com.taxsettle.service;

import com.taxsettle.entity.AnnualReview;
import com.taxsettle.entity.ReviewRisk;
import com.taxsettle.entity.TaxSettlement;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.RiskLevel;
import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.repository.AnnualReviewRepository;
import com.taxsettle.repository.TaxSettlementRepository;
import com.taxsettle.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final TaxSettlementRepository taxSettlementRepository;
    private final AnnualReviewRepository annualReviewRepository;
    private final TaxpayerRepository taxpayerRepository;

    public Map<String, Object> summaryByEmployer(Long employerId, Integer taxYear) {
        List<TaxSettlement> settlements = taxSettlementRepository.findByEmployerIdAndTaxYear(employerId, taxYear)
                .stream()
                .filter(TaxSettlement::getLocked)
                .toList();
        return buildSummary(settlements);
    }

    public Map<String, Object> summaryByType(SettlementStatus status, Integer taxYear) {
        List<TaxSettlement> settlements = taxSettlementRepository.findByResultStatusAndTaxYear(status, taxYear)
                .stream()
                .filter(TaxSettlement::getLocked)
                .toList();
        return buildSummary(settlements);
    }

    public Map<String, Object> fullSummary(Integer taxYear) {
        List<TaxSettlement> settlements = taxSettlementRepository.findByTaxYear(taxYear)
                .stream()
                .filter(TaxSettlement::getLocked)
                .toList();
        Map<String, Object> result = buildSummary(settlements);

        Map<String, Long> countByStatus = new LinkedHashMap<>();
        Map<String, BigDecimal> diffByStatus = new LinkedHashMap<>();
        for (SettlementStatus s : SettlementStatus.values()) {
            List<TaxSettlement> filtered = settlements.stream()
                    .filter(st -> st.getResultStatus() == s)
                    .toList();
            countByStatus.put(s.name(), (long) filtered.size());
            diffByStatus.put(s.name(), filtered.stream()
                    .map(TaxSettlement::getDifference)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        result.put("countByStatus", countByStatus);
        result.put("differenceByStatus", diffByStatus);
        return result;
    }

    public Map<String, Object> summaryByEmployerAndRiskLevel(Long employerId, Integer taxYear) {
        List<AnnualReview> reviews = annualReviewRepository.findByEmployerIdAndTaxYear(employerId, taxYear);
        return buildRiskGroupedSummary(reviews, taxYear);
    }

    public Map<String, Object> fullSummaryByRiskLevel(Integer taxYear) {
        List<AnnualReview> reviews = annualReviewRepository.findByTaxYear(taxYear);
        return buildRiskGroupedSummary(reviews, taxYear);
    }

    public List<Map<String, Object>> exportByEmployerAndRiskLevel(Long employerId, Integer taxYear) {
        List<AnnualReview> reviews = annualReviewRepository.findByEmployerIdAndTaxYear(employerId, taxYear);
        return buildExportRows(reviews);
    }

    public List<Map<String, Object>> exportByRiskLevel(Integer taxYear, RiskLevel riskLevel) {
        List<AnnualReview> reviews = annualReviewRepository.findByOverallRiskLevelAndTaxYear(riskLevel, taxYear);
        return buildExportRows(reviews);
    }

    public List<Map<String, Object>> exportAllByRiskLevel(Integer taxYear) {
        List<AnnualReview> reviews = annualReviewRepository.findByTaxYear(taxYear);
        return buildExportRows(reviews);
    }

    private Map<String, Object> buildRiskGroupedSummary(List<AnnualReview> reviews, Integer taxYear) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (RiskLevel level : RiskLevel.values()) {
            List<AnnualReview> filtered = reviews.stream()
                    .filter(r -> r.getOverallRiskLevel() == level)
                    .toList();

            List<Long> taxpayerIds = filtered.stream()
                    .map(r -> r.getTaxpayer().getId())
                    .toList();

            List<TaxSettlement> settlements = taxSettlementRepository.findByTaxYear(taxYear).stream()
                    .filter(s -> taxpayerIds.contains(s.getTaxpayer().getId()))
                    .toList();

            Map<String, Object> groupSummary = buildSummary(settlements);

            Map<String, Long> riskTypeCounts = new LinkedHashMap<>();
            for (AnnualReview r : filtered) {
                for (ReviewRisk risk : r.getRisks()) {
                    riskTypeCounts.merge(risk.getRiskType().name(), 1L, Long::sum);
                }
            }
            groupSummary.put("riskTypeCounts", riskTypeCounts);
            groupSummary.put("reviewedCount", filtered.stream().filter(AnnualReview::getReviewed).count());
            groupSummary.put("passedCount", filtered.stream()
                    .filter(r -> r.getReviewConclusion() == SettlementStatus.REVIEW_PASSED).count());
            groupSummary.put("failedCount", filtered.stream()
                    .filter(r -> r.getReviewConclusion() == SettlementStatus.REVIEW_FAILED).count());

            result.put(level.name(), groupSummary);
        }

        result.put("totalCount", reviews.size());
        return result;
    }

    private List<Map<String, Object>> buildExportRows(List<AnnualReview> reviews) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AnnualReview review : reviews) {
            Taxpayer tp = review.getTaxpayer();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("taxYear", review.getTaxYear());
            row.put("taxpayerName", tp.getName());
            row.put("taxpayerIdNumber", tp.getIdNumber());
            row.put("employerName", tp.getEmployer() != null ? tp.getEmployer().getName() : "");
            row.put("overallRiskLevel", review.getOverallRiskLevel().name());
            row.put("employerCount", review.getEmployerCount());
            row.put("totalIncome", review.getTotalIncomeFromAllEmployers());
            row.put("incomeFluctuationRate", review.getIncomeFluctuationRate());
            row.put("prepaidTaxCoverageRate", review.getPrepaidTaxCoverageRate());
            row.put("duplicateDeductionCount", review.getDuplicateDeductionCount());
            row.put("taxpayerStatus", tp.getStatus().name());
            row.put("reviewed", review.getReviewed());
            row.put("reviewConclusion", review.getReviewConclusion() != null ? review.getReviewConclusion().name() : "");
            row.put("reviewerRemark", review.getReviewerRemark() != null ? review.getReviewerRemark() : "");
            row.put("riskCount", review.getRisks().size());

            String riskDescriptions = review.getRisks().stream()
                    .map(r -> "[" + r.getRiskLevel() + "]" + r.getRiskType() + ": " + r.getDescription())
                    .collect(Collectors.joining("; "));
            row.put("riskDetails", riskDescriptions);

            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> buildSummary(List<TaxSettlement> settlements) {
        long totalCount = settlements.size();
        BigDecimal totalComprehensiveIncome = settlements.stream()
                .map(TaxSettlement::getComprehensiveIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTaxableIncome = settlements.stream()
                .map(TaxSettlement::getTaxableIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTaxPayable = settlements.stream()
                .map(TaxSettlement::getAnnualTaxPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrepaid = settlements.stream()
                .map(TaxSettlement::getTotalPrepaidTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDifference = settlements.stream()
                .map(TaxSettlement::getDifference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long owesCount = settlements.stream()
                .filter(s -> s.getResultStatus() == SettlementStatus.OWES_TAX)
                .count();
        long refundCount = settlements.stream()
                .filter(s -> s.getResultStatus() == SettlementStatus.DUE_REFUND)
                .count();
        long exemptCount = settlements.stream()
                .filter(s -> s.getExempt())
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("totalComprehensiveIncome", totalComprehensiveIncome);
        summary.put("totalTaxableIncome", totalTaxableIncome);
        summary.put("totalTaxPayable", totalTaxPayable);
        summary.put("totalPrepaidTax", totalPrepaid);
        summary.put("totalDifference", totalDifference);
        summary.put("owesTaxCount", owesCount);
        summary.put("refundCount", refundCount);
        summary.put("exemptCount", exemptCount);
        return summary;
    }
}
