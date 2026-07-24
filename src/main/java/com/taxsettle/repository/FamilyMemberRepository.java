package com.taxsettle.repository;

import com.taxsettle.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByTaxpayerId(Long taxpayerId);
    Optional<FamilyMember> findByIdNumber(String idNumber);
    List<FamilyMember> findByIdNumberAndTaxpayerIdNot(String idNumber, Long excludeTaxpayerId);
}
