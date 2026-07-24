package com.taxsettle.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.taxsettle.entity.enums.RiskLevel;
import com.taxsettle.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "annual_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AnnualReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Taxpayer taxpayer;

    @Column(nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RiskLevel overallRiskLevel = RiskLevel.NONE;

    @Column
    private Integer employerCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalIncomeFromAllEmployers;

    @Column(precision = 12, scale = 2)
    private BigDecimal incomeFluctuationRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal prepaidTaxCoverageRate;

    @Column
    private Integer duplicateDeductionCount;

    @Enumerated(EnumType.STRING)
    @Column
    private SettlementStatus reviewConclusion;

    @Column
    private String reviewerRemark;

    @Column(nullable = false)
    @Builder.Default
    private Boolean reviewed = false;

    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "annualReview", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<ReviewRisk> risks = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
