package com.taxsettle.repository;

import com.taxsettle.entity.ReviewRisk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRiskRepository extends JpaRepository<ReviewRisk, Long> {
    List<ReviewRisk> findByAnnualReviewId(Long annualReviewId);
    void deleteByAnnualReviewId(Long annualReviewId);
}
