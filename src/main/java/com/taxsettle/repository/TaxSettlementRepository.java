package com.taxsettle.repository;

import com.taxsettle.entity.TaxSettlement;
import com.taxsettle.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaxSettlementRepository extends JpaRepository<TaxSettlement, Long> {
    Optional<TaxSettlement> findByTaxpayerIdAndTaxYear(Long taxpayerId, Integer taxYear);
    List<TaxSettlement> findByTaxYear(Integer taxYear);
    List<TaxSettlement> findByResultStatusAndTaxYear(SettlementStatus status, Integer taxYear);

    @Query("SELECT s FROM TaxSettlement s WHERE s.taxpayer.employer.id = :employerId AND s.taxYear = :taxYear")
    List<TaxSettlement> findByEmployerIdAndTaxYear(@Param("employerId") Long employerId, @Param("taxYear") Integer taxYear);

    @Query("SELECT s FROM TaxSettlement s JOIN s.taxpayer t WHERE t.employer.id = :employerId")
    List<TaxSettlement> findByEmployerId(@Param("employerId") Long employerId);
}
