package com.taxsettle.repository;

import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxpayerRepository extends JpaRepository<Taxpayer, Long> {
    List<Taxpayer> findByEmployerId(Long employerId);
    List<Taxpayer> findByStatus(SettlementStatus status);
    List<Taxpayer> findByTaxYear(Integer taxYear);
    List<Taxpayer> findByEmployerIdAndTaxYear(Long employerId, Integer taxYear);
}
