package com.taxsettle.service;

import com.taxsettle.entity.*;
import com.taxsettle.entity.enums.IncomeType;
import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.repository.IncomeRecordRepository;
import com.taxsettle.repository.SpecialDeductionRepository;
import com.taxsettle.repository.TaxSettlementRepository;
import com.taxsettle.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxCalculationService {
    private final TaxpayerRepository taxpayerRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final SpecialDeductionRepository specialDeductionRepository;
    private final TaxSettlementRepository taxSettlementRepository;
    private final AnnualReviewService annualReviewService;

    @Value("${tax.threshold:60000}")
    private BigDecimal threshold;

    @Value("${tax.exempt-amount:400}")
    private BigDecimal exemptAmount;

    private static final BigDecimal WAGES_RATIO = BigDecimal.ONE;
    private static final BigDecimal LABOR_RATIO = new BigDecimal("0.8");
    private static final BigDecimal AUTHOR_RATIO = new BigDecimal("0.56");
    private static final BigDecimal ROYALTIES_RATIO = new BigDecimal("0.8");

    private static final int[][] TAX_BRACKETS = {
            {0, 36000, 3, 0},
            {36000, 144000, 10, 2520},
            {144000, 300000, 20, 16920},
            {300000, 420000, 25, 31920},
            {420000, 660000, 30, 52920},
            {660000, 960000, 35, 85920},
            {960000, Integer.MAX_VALUE, 45, 181920}
    };

    @Transactional
    public TaxSettlement calculate(Long taxpayerId) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));

        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot recalculate");
        }

        taxSettlementRepository.findByTaxpayerIdAndTaxYear(taxpayerId, taxpayer.getTaxYear())
                .ifPresent(existingSettlement -> {
                    if (existingSettlement.getLocked()) {
                        throw new IllegalStateException("Settlement record is locked, cannot recalculate");
                    }
                });

        taxpayer.setStatus(SettlementStatus.CALCULATING);
        taxpayerRepository.save(taxpayer);

        List<IncomeRecord> incomes = incomeRecordRepository.findByTaxpayerId(taxpayerId);
        List<SpecialDeduction> deductions = specialDeductionRepository.findByTaxpayerId(taxpayerId);

        BigDecimal comprehensiveIncome = calculateComprehensiveIncome(incomes);
        BigDecimal totalPrepaidTax = incomes.stream()
                .map(IncomeRecord::getPrepaidTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal specialDeductionTotal = deductions.stream()
                .map(SpecialDeduction::getAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal socialInsurance = taxpayer.getSocialInsuranceDeduction() != null
                ? taxpayer.getSocialInsuranceDeduction() : BigDecimal.ZERO;

        BigDecimal taxableIncome = comprehensiveIncome
                .subtract(threshold)
                .subtract(socialInsurance)
                .subtract(specialDeductionTotal)
                .max(BigDecimal.ZERO);

        int[] bracket = findBracket(taxableIncome);
        int taxRatePercent = bracket[2];
        BigDecimal quickDeduction = new BigDecimal(bracket[3]);

        BigDecimal annualTaxPayable = taxableIncome
                .multiply(new BigDecimal(taxRatePercent))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                .subtract(quickDeduction)
                .max(BigDecimal.ZERO);

        BigDecimal difference = annualTaxPayable.subtract(totalPrepaidTax);

        SettlementStatus resultStatus;
        boolean exempt = false;
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            if (difference.compareTo(exemptAmount) <= 0) {
                exempt = true;
                resultStatus = SettlementStatus.EXEMPT;
            } else {
                resultStatus = SettlementStatus.OWES_TAX;
            }
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            resultStatus = SettlementStatus.DUE_REFUND;
        } else {
            exempt = true;
            resultStatus = SettlementStatus.EXEMPT;
        }

        TaxSettlement settlement = taxSettlementRepository
                .findByTaxpayerIdAndTaxYear(taxpayerId, taxpayer.getTaxYear())
                .orElse(TaxSettlement.builder()
                        .taxpayer(taxpayer)
                        .taxYear(taxpayer.getTaxYear())
                        .build());

        settlement.setComprehensiveIncome(comprehensiveIncome);
        settlement.setThresholdDeduction(threshold);
        settlement.setSocialInsuranceDeduction(socialInsurance);
        settlement.setSpecialDeductionTotal(specialDeductionTotal);
        settlement.setTaxableIncome(taxableIncome);
        settlement.setTaxRatePercent(taxRatePercent);
        settlement.setQuickDeduction(quickDeduction);
        settlement.setAnnualTaxPayable(annualTaxPayable);
        settlement.setTotalPrepaidTax(totalPrepaidTax);
        settlement.setDifference(difference);
        settlement.setResultStatus(resultStatus);
        settlement.setExempt(exempt);
        settlement.setCalculatedAt(LocalDateTime.now());

        TaxSettlement savedSettlement = taxSettlementRepository.save(settlement);

        annualReviewService.performReview(taxpayerId, taxpayer.getTaxYear());

        return savedSettlement;
    }

    private BigDecimal calculateComprehensiveIncome(List<IncomeRecord> incomes) {
        BigDecimal total = BigDecimal.ZERO;
        for (IncomeRecord income : incomes) {
            BigDecimal ratio = switch (income.getIncomeType()) {
                case WAGES -> WAGES_RATIO;
                case LABOR_REMUNERATION -> LABOR_RATIO;
                case AUTHOR_REMUNERATION -> AUTHOR_RATIO;
                case ROYALTIES -> ROYALTIES_RATIO;
            };
            total = total.add(income.getGrossIncome().multiply(ratio));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int[] findBracket(BigDecimal taxableIncome) {
        int income = taxableIncome.intValue();
        for (int[] bracket : TAX_BRACKETS) {
            if (income >= bracket[0] && income < bracket[1]) {
                return bracket;
            }
        }
        return TAX_BRACKETS[TAX_BRACKETS.length - 1];
    }
}
