package com.taxsettle.service;

import com.taxsettle.entity.FamilyMember;
import com.taxsettle.entity.SpecialDeduction;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.DeductionType;
import com.taxsettle.repository.FamilyMemberRepository;
import com.taxsettle.repository.SpecialDeductionRepository;
import com.taxsettle.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpecialDeductionService {
    private final SpecialDeductionRepository specialDeductionRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final FamilyMemberRepository familyMemberRepository;

    private static final BigDecimal CHILDREN_EDUCATION_LIMIT = new BigDecimal("24000");
    private static final BigDecimal ELDERLY_SUPPORT_ONLY_CHILD_LIMIT = new BigDecimal("36000");
    private static final BigDecimal ELDERLY_SUPPORT_SHARED_LIMIT = new BigDecimal("18000");
    private static final BigDecimal HOUSING_LOAN_LIMIT = new BigDecimal("12000");
    private static final BigDecimal CONTINUING_EDUCATION_DEGREE_LIMIT = new BigDecimal("4800");
    private static final BigDecimal CONTINUING_EDUCATION_CERTIFICATION_LIMIT = new BigDecimal("3600");

    public List<SpecialDeduction> findByTaxpayerId(Long taxpayerId) {
        return specialDeductionRepository.findByTaxpayerId(taxpayerId);
    }

    @Transactional
    public SpecialDeduction create(SpecialDeduction deduction) {
        Taxpayer taxpayer = taxpayerRepository.findById(deduction.getTaxpayer().getId())
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        ensureNotLocked(taxpayer);

        if (specialDeductionRepository.existsByTaxpayerIdAndDeductionType(
                taxpayer.getId(), deduction.getDeductionType())) {
            throw new IllegalStateException(
                    "Duplicate deduction type: " + deduction.getDeductionType()
                            + " already exists for taxpayer " + taxpayer.getId());
        }

        deduction.setTaxpayer(taxpayer);
        applyLimitAndFamilyMember(deduction);
        return specialDeductionRepository.save(deduction);
    }

    @Transactional
    public List<SpecialDeduction> createBatch(Long taxpayerId, List<SpecialDeduction> deductions) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        ensureNotLocked(taxpayer);

        Set<DeductionType> typesInBatch = new HashSet<>();
        for (SpecialDeduction d : deductions) {
            if (!typesInBatch.add(d.getDeductionType())) {
                throw new IllegalStateException(
                        "Duplicate deduction type within batch: " + d.getDeductionType());
            }
            if (specialDeductionRepository.existsByTaxpayerIdAndDeductionType(
                    taxpayerId, d.getDeductionType())) {
                throw new IllegalStateException(
                        "Duplicate deduction type: " + d.getDeductionType()
                                + " already exists for taxpayer " + taxpayerId);
            }
        }

        for (SpecialDeduction d : deductions) {
            d.setTaxpayer(taxpayer);
            applyLimitAndFamilyMember(d);
        }
        return specialDeductionRepository.saveAll(deductions);
    }

    @Transactional
    public SpecialDeduction update(Long id, SpecialDeduction updated) {
        SpecialDeduction existing = specialDeductionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special deduction not found: " + id));
        ensureNotLocked(existing.getTaxpayer());

        if (updated.getDeductionType() != null
                && !updated.getDeductionType().equals(existing.getDeductionType())) {
            specialDeductionRepository
                    .findByTaxpayerIdAndDeductionTypeAndIdNot(
                            existing.getTaxpayer().getId(), updated.getDeductionType(), id)
                    .ifPresent(conflict -> {
                        throw new IllegalStateException(
                                "Duplicate deduction type after update: " + updated.getDeductionType()
                                        + " already exists for taxpayer " + existing.getTaxpayer().getId());
                    });
            existing.setDeductionType(updated.getDeductionType());
        }

        if (updated.getAnnualAmount() != null) {
            existing.setAnnualAmount(updated.getAnnualAmount());
        }
        existing.setAnnualLimit(resolveLimit(existing.getDeductionType(), existing.getAnnualAmount()));
        if (existing.getAnnualAmount().compareTo(existing.getAnnualLimit()) > 0) {
            existing.setAnnualAmount(existing.getAnnualLimit());
        }
        existing.setRemark(updated.getRemark());
        if (updated.getFamilyMember() != null && updated.getFamilyMember().getId() != null) {
            FamilyMember fm = familyMemberRepository.findById(updated.getFamilyMember().getId())
                    .orElseThrow(() -> new RuntimeException("Family member not found"));
            existing.setFamilyMember(fm);
        } else {
            existing.setFamilyMember(null);
        }
        return specialDeductionRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        SpecialDeduction existing = specialDeductionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special deduction not found: " + id));
        ensureNotLocked(existing.getTaxpayer());
        specialDeductionRepository.deleteById(id);
    }

    @Transactional
    public void deleteByTaxpayerId(Long taxpayerId) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));
        ensureNotLocked(taxpayer);
        specialDeductionRepository.deleteByTaxpayerId(taxpayerId);
    }

    private void ensureNotLocked(Taxpayer taxpayer) {
        if (Boolean.TRUE.equals(taxpayer.getLocked())) {
            throw new IllegalStateException(
                    "Taxpayer " + taxpayer.getId() + " is locked and cannot be modified");
        }
    }

    private void applyLimitAndFamilyMember(SpecialDeduction d) {
        d.setAnnualLimit(resolveLimit(d.getDeductionType(), d.getAnnualAmount()));
        if (d.getAnnualAmount().compareTo(d.getAnnualLimit()) > 0) {
            d.setAnnualAmount(d.getAnnualLimit());
        }
        if (d.getFamilyMember() != null && d.getFamilyMember().getId() != null) {
            FamilyMember fm = familyMemberRepository.findById(d.getFamilyMember().getId())
                    .orElseThrow(() -> new RuntimeException("Family member not found"));
            d.setFamilyMember(fm);
        }
    }

    private BigDecimal resolveLimit(DeductionType type, BigDecimal declaredAmount) {
        return switch (type) {
            case CHILDREN_EDUCATION -> CHILDREN_EDUCATION_LIMIT;
            case ELDERLY_SUPPORT -> {
                if (declaredAmount.compareTo(ELDERLY_SUPPORT_ONLY_CHILD_LIMIT) >= 0) {
                    yield ELDERLY_SUPPORT_ONLY_CHILD_LIMIT;
                }
                yield ELDERLY_SUPPORT_SHARED_LIMIT;
            }
            case HOUSING_LOAN_INTEREST -> HOUSING_LOAN_LIMIT;
            case CONTINUING_EDUCATION_DEGREE -> CONTINUING_EDUCATION_DEGREE_LIMIT;
            case CONTINUING_EDUCATION_CERTIFICATION -> CONTINUING_EDUCATION_CERTIFICATION_LIMIT;
        };
    }
}
