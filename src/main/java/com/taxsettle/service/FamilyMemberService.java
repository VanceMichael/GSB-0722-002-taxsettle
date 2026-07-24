package com.taxsettle.service;

import com.taxsettle.entity.FamilyMember;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.repository.FamilyMemberRepository;
import com.taxsettle.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyMemberService {
    private final FamilyMemberRepository familyMemberRepository;
    private final TaxpayerRepository taxpayerRepository;

    public List<FamilyMember> findByTaxpayerId(Long taxpayerId) {
        return familyMemberRepository.findByTaxpayerId(taxpayerId);
    }

    public FamilyMember findById(Long id) {
        return familyMemberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Family member not found: " + id));
    }

    @Transactional
    public FamilyMember create(Long taxpayerId, FamilyMember familyMember) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked");
        }
        familyMember.setTaxpayer(taxpayer);
        return familyMemberRepository.save(familyMember);
    }

    @Transactional
    public FamilyMember update(Long id, FamilyMember updated) {
        FamilyMember existing = findById(id);
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked");
        }
        existing.setName(updated.getName());
        existing.setIdNumber(updated.getIdNumber());
        existing.setRelationship(updated.getRelationship());
        existing.setRemark(updated.getRemark());
        return familyMemberRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        FamilyMember existing = findById(id);
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked");
        }
        familyMemberRepository.deleteById(id);
    }
}
