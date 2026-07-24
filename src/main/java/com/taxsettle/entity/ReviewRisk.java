package com.taxsettle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxsettle.entity.enums.RiskLevel;
import com.taxsettle.entity.enums.RiskType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "review_risks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReviewRisk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "annual_review_id", nullable = false)
    @JsonIgnore
    private AnnualReview annualReview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskType riskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal relatedAmount;

    @Column
    private String detail;
}
