package com.taxsettle.repository;

import com.taxsettle.entity.IncomeRecord;
import com.taxsettle.entity.enums.IncomeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncomeRecordRepository extends JpaRepository<IncomeRecord, Long> {
    List<IncomeRecord> findByTaxpayerId(Long taxpayerId);
    List<IncomeRecord> findByTaxpayerIdAndIncomeType(Long taxpayerId, IncomeType incomeType);
    void deleteByTaxpayerId(Long taxpayerId);

    @Query("SELECT DISTINCT r.employer.id FROM IncomeRecord r WHERE r.taxpayer.id = :taxpayerId AND r.employer IS NOT NULL")
    List<Long> findDistinctEmployerIdsByTaxpayerId(@Param("taxpayerId") Long taxpayerId);
}
