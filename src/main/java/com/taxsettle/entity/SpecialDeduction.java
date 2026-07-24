package com.taxsettle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxsettle.entity.enums.DeductionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "special_deductions",
        uniqueConstraints = @UniqueConstraint(name = "uk_special_deduction_taxpayer_type",
                columnNames = {"taxpayer_id", "deduction_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SpecialDeduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    @JsonIgnore
    private Taxpayer taxpayer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "family_member_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private FamilyMember familyMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionType deductionType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal annualAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal annualLimit;

    private String remark;
}
