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
import java.util.EnumSet;
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
    public SpecialDeduction create(Long taxpayerId, SpecialDeduction deduction) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add deductions");
        }
        // Ownership comes from the path taxpayer, never from the request body.
        deduction.setTaxpayer(taxpayer);
        if (specialDeductionRepository
                .existsByTaxpayerIdAndDeductionType(taxpayer.getId(), deduction.getDeductionType())) {
            throw new DuplicateDeductionException(
                    "Deduction type " + deduction.getDeductionType()
                            + " already exists for taxpayer " + taxpayer.getId());
        }
        deduction.setAnnualLimit(resolveLimit(deduction.getDeductionType(), deduction.getAnnualAmount()));
        if (deduction.getAnnualAmount().compareTo(deduction.getAnnualLimit()) > 0) {
            deduction.setAnnualAmount(deduction.getAnnualLimit());
        }
        deduction.setFamilyMember(resolveOwnedFamilyMember(taxpayer, deduction.getFamilyMember()));
        return specialDeductionRepository.save(deduction);
    }

    @Transactional
    public List<SpecialDeduction> createBatch(Long taxpayerId, List<SpecialDeduction> deductions) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add deductions");
        }
        // Validate the whole batch first so a single conflict aborts the entire write.
        // A type conflicts if it already exists in the DB or is repeated within the batch.
        Set<DeductionType> seenInBatch = EnumSet.noneOf(DeductionType.class);
        for (SpecialDeduction d : deductions) {
            DeductionType type = d.getDeductionType();
            if (!seenInBatch.add(type)) {
                throw new DuplicateDeductionException(
                        "Deduction type " + type + " appears more than once in the batch");
            }
            if (specialDeductionRepository.existsByTaxpayerIdAndDeductionType(taxpayerId, type)) {
                throw new DuplicateDeductionException(
                        "Deduction type " + type + " already exists for taxpayer " + taxpayerId);
            }
        }
        deductions.forEach(d -> {
            d.setTaxpayer(taxpayer);
            d.setAnnualLimit(resolveLimit(d.getDeductionType(), d.getAnnualAmount()));
            if (d.getAnnualAmount().compareTo(d.getAnnualLimit()) > 0) {
                d.setAnnualAmount(d.getAnnualLimit());
            }
            d.setFamilyMember(resolveOwnedFamilyMember(taxpayer, d.getFamilyMember()));
        });
        return specialDeductionRepository.saveAll(deductions);
    }

    @Transactional
    public SpecialDeduction update(Long taxpayerId, Long id, SpecialDeduction updated) {
        SpecialDeduction existing = specialDeductionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special deduction not found: " + id));
        // The record must belong to the taxpayer in the path, otherwise this is a cross-taxpayer edit.
        if (!existing.getTaxpayer().getId().equals(taxpayerId)) {
            throw new DeductionOwnershipException(
                    "Deduction " + id + " does not belong to taxpayer " + taxpayerId);
        }
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot modify deductions");
        }
        // Changing the type must not collide with another record of the same taxpayer.
        if (updated.getDeductionType() != existing.getDeductionType()) {
            specialDeductionRepository
                    .findByTaxpayerIdAndDeductionType(
                            existing.getTaxpayer().getId(), updated.getDeductionType())
                    .filter(other -> !other.getId().equals(existing.getId()))
                    .ifPresent(other -> {
                        throw new DuplicateDeductionException(
                                "Deduction type " + updated.getDeductionType()
                                        + " already exists for taxpayer "
                                        + existing.getTaxpayer().getId());
                    });
        }
        existing.setDeductionType(updated.getDeductionType());
        existing.setAnnualAmount(updated.getAnnualAmount());
        existing.setAnnualLimit(resolveLimit(updated.getDeductionType(), updated.getAnnualAmount()));
        if (existing.getAnnualAmount().compareTo(existing.getAnnualLimit()) > 0) {
            existing.setAnnualAmount(existing.getAnnualLimit());
        }
        existing.setRemark(updated.getRemark());
        existing.setFamilyMember(resolveOwnedFamilyMember(existing.getTaxpayer(), updated.getFamilyMember()));
        return specialDeductionRepository.save(existing);
    }

    @Transactional
    public void delete(Long taxpayerId, Long id) {
        SpecialDeduction existing = specialDeductionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special deduction not found: " + id));
        // The record must belong to the taxpayer in the path, otherwise this is a cross-taxpayer delete.
        if (!existing.getTaxpayer().getId().equals(taxpayerId)) {
            throw new DeductionOwnershipException(
                    "Deduction " + id + " does not belong to taxpayer " + taxpayerId);
        }
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

    /**
     * Resolves the referenced family member and enforces that it belongs to the
     * current taxpayer. Returns null when no family member is referenced.
     */
    private FamilyMember resolveOwnedFamilyMember(Taxpayer taxpayer, FamilyMember requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        FamilyMember fm = familyMemberRepository.findById(requested.getId())
                .orElseThrow(() -> new RuntimeException("Family member not found"));
        if (fm.getTaxpayer() == null || !fm.getTaxpayer().getId().equals(taxpayer.getId())) {
            throw new FamilyMemberOwnershipException(
                    "Family member " + requested.getId()
                            + " does not belong to taxpayer " + taxpayer.getId());
        }
        return fm;
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
