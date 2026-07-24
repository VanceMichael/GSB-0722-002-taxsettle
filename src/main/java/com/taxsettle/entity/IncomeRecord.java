package com.taxsettle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxsettle.entity.enums.IncomeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "income_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IncomeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    @JsonIgnore
    private Taxpayer taxpayer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Employer employer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomeType incomeType;

    @Column(name = "income_month", nullable = false)
    private Integer month;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossIncome;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal prepaidTax = BigDecimal.ZERO;
}
