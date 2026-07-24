package com.taxsettle.service;

import com.taxsettle.entity.Employer;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.repository.EmployerRepository;
import com.taxsettle.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxpayerService {
    private final TaxpayerRepository taxpayerRepository;
    private final EmployerRepository employerRepository;

    public List<Taxpayer> findAll() {
        return taxpayerRepository.findAll();
    }

    public Taxpayer findById(Long id) {
        return taxpayerRepository.findById(id).orElseThrow(() -> new RuntimeException("Taxpayer not found: " + id));
    }

    public List<Taxpayer> findByEmployerId(Long employerId) {
        return taxpayerRepository.findByEmployerId(employerId);
    }

    public List<Taxpayer> findByStatus(SettlementStatus status) {
        return taxpayerRepository.findByStatus(status);
    }

    public List<Taxpayer> findByTaxYear(Integer taxYear) {
        return taxpayerRepository.findByTaxYear(taxYear);
    }

    public Taxpayer create(Taxpayer taxpayer) {
        if (taxpayer.getSocialInsuranceDeduction() == null) {
            taxpayer.setSocialInsuranceDeduction(BigDecimal.ZERO);
        }
        return taxpayerRepository.save(taxpayer);
    }

    public Taxpayer update(Long id, Taxpayer updated) {
        Taxpayer existing = findById(id);
        if (existing.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked and cannot be modified");
        }
        existing.setName(updated.getName());
        existing.setIdNumber(updated.getIdNumber());
        existing.setTaxYear(updated.getTaxYear());
        existing.setSocialInsuranceDeduction(updated.getSocialInsuranceDeduction());
        if (updated.getEmployer() != null && updated.getEmployer().getId() != null) {
            Employer employer = employerRepository.findById(updated.getEmployer().getId())
                    .orElseThrow(() -> new RuntimeException("Employer not found"));
            existing.setEmployer(employer);
        }
        return taxpayerRepository.save(existing);
    }

    public void delete(Long id) {
        Taxpayer existing = findById(id);
        if (existing.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked and cannot be deleted");
        }
        taxpayerRepository.deleteById(id);
    }

    public Taxpayer updateStatus(Long id, SettlementStatus status) {
        Taxpayer existing = findById(id);
        if (existing.getLocked() && status != SettlementStatus.SETTLED) {
            throw new IllegalStateException("Taxpayer record is locked");
        }
        existing.setStatus(status);
        return taxpayerRepository.save(existing);
    }

    public Taxpayer lock(Long id) {
        Taxpayer existing = findById(id);
        existing.setLocked(true);
        return taxpayerRepository.save(existing);
    }
}
