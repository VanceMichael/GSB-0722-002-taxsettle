package com.taxsettle.repository;

import com.taxsettle.entity.AnnualReview;
import com.taxsettle.entity.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnnualReviewRepository extends JpaRepository<AnnualReview, Long> {
    Optional<AnnualReview> findByTaxpayerIdAndTaxYear(Long taxpayerId, Integer taxYear);
    List<AnnualReview> findByTaxYear(Integer taxYear);
    List<AnnualReview> findByOverallRiskLevelAndTaxYear(RiskLevel riskLevel, Integer taxYear);

    @Query("SELECT r FROM AnnualReview r WHERE r.taxpayer.employer.id = :employerId AND r.taxYear = :taxYear")
    List<AnnualReview> findByEmployerIdAndTaxYear(@Param("employerId") Long employerId, @Param("taxYear") Integer taxYear);

    @Query("SELECT r FROM AnnualReview r WHERE r.taxpayer.employer.id = :employerId AND r.overallRiskLevel = :riskLevel AND r.taxYear = :taxYear")
    List<AnnualReview> findByEmployerIdAndRiskLevelAndTaxYear(@Param("employerId") Long employerId, @Param("riskLevel") RiskLevel riskLevel, @Param("taxYear") Integer taxYear);
}
