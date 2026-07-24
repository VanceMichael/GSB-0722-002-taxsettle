package com.taxsettle.service;

import com.taxsettle.entity.TaxSettlement;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.repository.TaxSettlementRepository;
import com.taxsettle.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxSettlementService {
    private final TaxSettlementRepository taxSettlementRepository;
    private final TaxpayerRepository taxpayerRepository;

    public TaxSettlement findByTaxpayerIdAndTaxYear(Long taxpayerId, Integer taxYear) {
        return taxSettlementRepository.findByTaxpayerIdAndTaxYear(taxpayerId, taxYear)
                .orElseThrow(() -> new RuntimeException("Settlement not found for taxpayer: " + taxpayerId + ", year: " + taxYear));
    }

    public List<TaxSettlement> findByTaxYear(Integer taxYear) {
        return taxSettlementRepository.findByTaxYear(taxYear);
    }

    public List<TaxSettlement> findByEmployerIdAndTaxYear(Long employerId, Integer taxYear) {
        return taxSettlementRepository.findByEmployerIdAndTaxYear(employerId, taxYear);
    }

    @Transactional
    public TaxSettlement confirm(Long taxpayerId, Integer taxYear) {
        TaxSettlement settlement = findByTaxpayerIdAndTaxYear(taxpayerId, taxYear);

        SettlementStatus current = settlement.getResultStatus();
        if (current != SettlementStatus.OWES_TAX
                && current != SettlementStatus.DUE_REFUND
                && current != SettlementStatus.EXEMPT) {
            throw new IllegalStateException("Settlement is not in a confirmable state: " + current);
        }

        if (settlement.getLocked()) {
            throw new IllegalStateException("Settlement is already locked");
        }

        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));

        if (taxpayer.getStatus() == SettlementStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Taxpayer is under review, must conclude review first");
        }
        if (taxpayer.getStatus() == SettlementStatus.REVIEW_FAILED) {
            throw new IllegalStateException("Taxpayer review failed, cannot confirm settlement");
        }

        settlement.setLocked(true);
        settlement.setSettledAt(LocalDateTime.now());

        taxpayer.setStatus(SettlementStatus.SETTLED);
        taxpayer.setLocked(true);
        taxpayerRepository.save(taxpayer);

        return taxSettlementRepository.save(settlement);
    }

    public List<TaxSettlement> findByResultStatusAndTaxYear(SettlementStatus status, Integer taxYear) {
        return taxSettlementRepository.findByResultStatusAndTaxYear(status, taxYear);
    }
}
