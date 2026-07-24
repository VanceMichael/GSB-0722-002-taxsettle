package com.taxsettle.repository;

import com.taxsettle.entity.SpecialDeduction;
import com.taxsettle.entity.enums.DeductionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialDeductionRepository extends JpaRepository<SpecialDeduction, Long> {
    List<SpecialDeduction> findByTaxpayerId(Long taxpayerId);
    void deleteByTaxpayerId(Long taxpayerId);
    Optional<SpecialDeduction> findByTaxpayerIdAndDeductionType(Long taxpayerId, DeductionType deductionType);
    boolean existsByTaxpayerIdAndDeductionType(Long taxpayerId, DeductionType deductionType);
}
