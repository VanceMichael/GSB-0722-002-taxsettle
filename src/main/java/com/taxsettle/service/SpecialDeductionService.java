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

    @Transactional(readOnly = true)
    public List<SpecialDeduction> findByTaxpayerId(Long taxpayerId) {
        return specialDeductionRepository.findByTaxpayerId(taxpayerId);
    }

    @Transactional
    public SpecialDeduction create(Long taxpayerId, SpecialDeduction deduction) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add deductions");
        }
        if (deduction.getDeductionType() == null) {
            throw new IllegalArgumentException("Deduction type must not be null");
        }
        if (specialDeductionRepository.existsByTaxpayerIdAndDeductionType(
                taxpayer.getId(), deduction.getDeductionType())) {
            throw new IllegalStateException(
                    "Duplicate deduction type '" + deduction.getDeductionType()
                            + "' already exists for taxpayer " + taxpayer.getId());
        }
        deduction.setId(null);
        deduction.setTaxpayer(taxpayer);
        applyLimitAndCap(deduction);
        resolveFamilyMember(deduction);
        return specialDeductionRepository.save(deduction);
    }

    @Transactional
    public List<SpecialDeduction> createBatch(Long taxpayerId, List<SpecialDeduction> deductions) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add deductions");
        }

        Set<DeductionType> seenInBatch = new HashSet<>();
        for (SpecialDeduction d : deductions) {
            if (d.getDeductionType() == null) {
                throw new IllegalArgumentException("Deduction type must not be null");
            }
            if (!seenInBatch.add(d.getDeductionType())) {
                throw new IllegalStateException(
                        "Duplicate deduction type '" + d.getDeductionType()
                                + "' appears more than once in this batch");
            }
            if (specialDeductionRepository.existsByTaxpayerIdAndDeductionType(
                    taxpayerId, d.getDeductionType())) {
                throw new IllegalStateException(
                        "Duplicate deduction type '" + d.getDeductionType()
                                + "' already exists for taxpayer " + taxpayerId);
            }
        }

        for (SpecialDeduction d : deductions) {
            d.setTaxpayer(taxpayer);
            applyLimitAndCap(d);
            resolveFamilyMember(d);
        }
        return specialDeductionRepository.saveAll(deductions);
    }

    @Transactional
    public SpecialDeduction update(Long id, SpecialDeduction updated) {
        SpecialDeduction existing = specialDeductionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special deduction not found: " + id));
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot modify deductions");
        }
        if (updated.getDeductionType() != null
                && updated.getDeductionType() != existing.getDeductionType()
                && specialDeductionRepository.existsByTaxpayerIdAndDeductionTypeAndIdNot(
                        existing.getTaxpayer().getId(),
                        updated.getDeductionType(),
                        existing.getId())) {
            throw new IllegalStateException(
                    "Duplicate deduction type '" + updated.getDeductionType()
                            + "' already exists for taxpayer " + existing.getTaxpayer().getId());
        }
        existing.setDeductionType(updated.getDeductionType());
        existing.setAnnualAmount(updated.getAnnualAmount());
        existing.setAnnualLimit(resolveLimit(updated.getDeductionType(), updated.getAnnualAmount()));
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
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot delete deductions");
        }
        specialDeductionRepository.deleteById(id);
    }

    @Transactional
    public void deleteByTaxpayerId(Long taxpayerId) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot delete deductions");
        }
        specialDeductionRepository.deleteByTaxpayerId(taxpayerId);
    }

    private void applyLimitAndCap(SpecialDeduction deduction) {
        deduction.setAnnualLimit(resolveLimit(deduction.getDeductionType(), deduction.getAnnualAmount()));
        if (deduction.getAnnualAmount().compareTo(deduction.getAnnualLimit()) > 0) {
            deduction.setAnnualAmount(deduction.getAnnualLimit());
        }
    }

    private void resolveFamilyMember(SpecialDeduction deduction) {
        if (deduction.getFamilyMember() != null && deduction.getFamilyMember().getId() != null) {
            FamilyMember fm = familyMemberRepository.findById(deduction.getFamilyMember().getId())
                    .orElseThrow(() -> new RuntimeException("Family member not found"));
            deduction.setFamilyMember(fm);
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
