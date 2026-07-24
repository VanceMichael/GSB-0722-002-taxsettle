package com.taxsettle.repository;

import com.taxsettle.entity.SpecialDeduction;
import com.taxsettle.entity.enums.DeductionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialDeductionRepository extends JpaRepository<SpecialDeduction, Long> {
    List<SpecialDeduction> findByTaxpayerId(Long taxpayerId);
    boolean existsByTaxpayerIdAndDeductionType(Long taxpayerId, DeductionType deductionType);
    boolean existsByTaxpayerIdAndDeductionTypeAndIdNot(Long taxpayerId, DeductionType deductionType, Long id);
    void deleteByTaxpayerId(Long taxpayerId);
}
