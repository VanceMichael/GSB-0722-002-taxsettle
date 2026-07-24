package com.taxsettle.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxsettle.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_settlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaxSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Taxpayer taxpayer;

    @Column(nullable = false)
    private Integer taxYear;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal comprehensiveIncome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdDeduction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal socialInsuranceDeduction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal specialDeductionTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxableIncome;

    @Column(nullable = false)
    private int taxRatePercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quickDeduction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal annualTaxPayable;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrepaidTax;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus resultStatus;

    @Column(nullable = false)
    private Boolean exempt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;

    private LocalDateTime calculatedAt;

    private LocalDateTime settledAt;
}
