package com.taxsettle.controller;

import com.taxsettle.entity.TaxSettlement;
import com.taxsettle.entity.enums.RiskLevel;
import com.taxsettle.service.AnnualReviewService;
import com.taxsettle.service.TaxCalculationService;
import com.taxsettle.service.TaxSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.service.StatisticsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class TaxSettlementController {
    private final TaxCalculationService taxCalculationService;
    private final TaxSettlementService taxSettlementService;
    private final StatisticsService statisticsService;
    private final AnnualReviewService annualReviewService;

    @PostMapping("/calculate/{taxpayerId}")
    public TaxSettlement calculate(@PathVariable Long taxpayerId) {
        return taxCalculationService.calculate(taxpayerId);
    }

    @GetMapping("/{taxpayerId}")
    public TaxSettlement get(@PathVariable Long taxpayerId,
                             @RequestParam(defaultValue = "2025") Integer taxYear) {
        return taxSettlementService.findByTaxpayerIdAndTaxYear(taxpayerId, taxYear);
    }

    @GetMapping
    public List<TaxSettlement> list(@RequestParam(defaultValue = "2025") Integer taxYear) {
        return taxSettlementService.findByTaxYear(taxYear);
    }

    @PostMapping("/confirm/{taxpayerId}")
    public TaxSettlement confirm(@PathVariable Long taxpayerId,
                                 @RequestParam(defaultValue = "2025") Integer taxYear) {
        return taxSettlementService.confirm(taxpayerId, taxYear);
    }

    @GetMapping("/employer/{employerId}")
    public List<TaxSettlement> byEmployer(@PathVariable Long employerId,
                                          @RequestParam(defaultValue = "2025") Integer taxYear) {
        return taxSettlementService.findByEmployerIdAndTaxYear(employerId, taxYear);
    }

    @GetMapping("/statistics")
    public Map<String, Object> statistics(@RequestParam(defaultValue = "2025") Integer taxYear) {
        return statisticsService.fullSummary(taxYear);
    }

    @GetMapping("/statistics/employer/{employerId}")
    public Map<String, Object> statisticsByEmployer(@PathVariable Long employerId,
                                                    @RequestParam(defaultValue = "2025") Integer taxYear) {
        return statisticsService.summaryByEmployer(employerId, taxYear);
    }

    @GetMapping("/statistics/type/{status}")
    public Map<String, Object> statisticsByType(@PathVariable SettlementStatus status,
                                                @RequestParam(defaultValue = "2025") Integer taxYear) {
        return statisticsService.summaryByType(status, taxYear);
    }

    @GetMapping("/statistics/risk")
    public Map<String, Object> statisticsByRiskLevel(@RequestParam(defaultValue = "2025") Integer taxYear) {
        return statisticsService.fullSummaryByRiskLevel(taxYear);
    }

    @GetMapping("/statistics/employer/{employerId}/risk")
    public Map<String, Object> statisticsByEmployerAndRiskLevel(@PathVariable Long employerId,
                                                                 @RequestParam(defaultValue = "2025") Integer taxYear) {
        return statisticsService.summaryByEmployerAndRiskLevel(employerId, taxYear);
    }

    @GetMapping("/export/risk")
    public List<Map<String, Object>> exportByRiskLevel(@RequestParam(defaultValue = "2025") Integer taxYear,
                                                        @RequestParam(required = false) RiskLevel riskLevel) {
        if (riskLevel != null) {
            return statisticsService.exportByRiskLevel(taxYear, riskLevel);
        }
        return statisticsService.exportAllByRiskLevel(taxYear);
    }

    @GetMapping("/export/employer/{employerId}/risk")
    public List<Map<String, Object>> exportByEmployerAndRiskLevel(@PathVariable Long employerId,
                                                                    @RequestParam(defaultValue = "2025") Integer taxYear) {
        return statisticsService.exportByEmployerAndRiskLevel(employerId, taxYear);
    }
}
