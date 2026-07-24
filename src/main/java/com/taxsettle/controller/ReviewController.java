package com.taxsettle.controller;

import com.taxsettle.entity.AnnualReview;
import com.taxsettle.entity.enums.RiskLevel;
import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.service.AnnualReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final AnnualReviewService annualReviewService;

    @PostMapping("/perform/{taxpayerId}")
    public AnnualReview performReview(@PathVariable Long taxpayerId,
                                      @RequestParam(defaultValue = "2025") Integer taxYear) {
        return annualReviewService.performReview(taxpayerId, taxYear);
    }

    @GetMapping("/{taxpayerId}")
    public AnnualReview getByTaxpayer(@PathVariable Long taxpayerId,
                                      @RequestParam(defaultValue = "2025") Integer taxYear) {
        return annualReviewService.findByTaxpayerIdAndTaxYear(taxpayerId, taxYear);
    }

    @GetMapping
    public List<AnnualReview> list(@RequestParam(defaultValue = "2025") Integer taxYear,
                                   @RequestParam(required = false) RiskLevel riskLevel,
                                   @RequestParam(required = false) Long employerId) {
        if (riskLevel != null) return annualReviewService.findByRiskLevelAndTaxYear(riskLevel, taxYear);
        if (employerId != null) return annualReviewService.findByEmployerIdAndTaxYear(employerId, taxYear);
        return annualReviewService.findByTaxYear(taxYear);
    }

    @PostMapping("/{reviewId}/conclude")
    public AnnualReview concludeReview(@PathVariable Long reviewId,
                                       @RequestBody Map<String, String> body) {
        SettlementStatus conclusion = SettlementStatus.valueOf(body.get("conclusion"));
        String remark = body.getOrDefault("remark", "");
        return annualReviewService.concludeReview(reviewId, conclusion, remark);
    }
}
