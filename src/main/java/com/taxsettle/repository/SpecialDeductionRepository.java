package com.taxsettle.repository;

import com.taxsettle.entity.SpecialDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialDeductionRepository extends JpaRepository<SpecialDeduction, Long> {
    List<SpecialDeduction> findByTaxpayerId(Long taxpayerId);
    void deleteByTaxpayerId(Long taxpayerId);
}
