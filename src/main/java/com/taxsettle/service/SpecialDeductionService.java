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

import java.math.BigDecimal;
import java.util.List;

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

    public SpecialDeduction create(SpecialDeduction deduction) {
        Taxpayer taxpayer = taxpayerRepository.findById(deduction.getTaxpayer().getId())
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add deductions");
        }
        deduction.setAnnualLimit(resolveLimit(deduction.getDeductionType(), deduction.getAnnualAmount()));
        if (deduction.getAnnualAmount().compareTo(deduction.getAnnualLimit()) > 0) {
            deduction.setAnnualAmount(deduction.getAnnualLimit());
        }
        if (deduction.getFamilyMember() != null && deduction.getFamilyMember().getId() != null) {
            FamilyMember fm = familyMemberRepository.findById(deduction.getFamilyMember().getId())
                    .orElseThrow(() -> new RuntimeException("Family member not found"));
            deduction.setFamilyMember(fm);
        }
        return specialDeductionRepository.save(deduction);
    }

    public List<SpecialDeduction> createBatch(Long taxpayerId, List<SpecialDeduction> deductions) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add deductions");
        }
        deductions.forEach(d -> {
            d.setTaxpayer(taxpayer);
            d.setAnnualLimit(resolveLimit(d.getDeductionType(), d.getAnnualAmount()));
            if (d.getAnnualAmount().compareTo(d.getAnnualLimit()) > 0) {
                d.setAnnualAmount(d.getAnnualLimit());
            }
            if (d.getFamilyMember() != null && d.getFamilyMember().getId() != null) {
                FamilyMember fm = familyMemberRepository.findById(d.getFamilyMember().getId())
                        .orElseThrow(() -> new RuntimeException("Family member not found"));
                d.setFamilyMember(fm);
            }
        });
        return specialDeductionRepository.saveAll(deductions);
    }

    public SpecialDeduction update(Long id, SpecialDeduction updated) {
        SpecialDeduction existing = specialDeductionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special deduction not found: " + id));
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot modify deductions");
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

    public void delete(Long id) {
        SpecialDeduction existing = specialDeductionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special deduction not found: " + id));
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot delete deductions");
        }
        specialDeductionRepository.deleteById(id);
    }

    public void deleteByTaxpayerId(Long taxpayerId) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot delete deductions");
        }
        specialDeductionRepository.deleteByTaxpayerId(taxpayerId);
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
