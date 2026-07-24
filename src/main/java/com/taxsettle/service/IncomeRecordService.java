package com.taxsettle.service;

import com.taxsettle.entity.Employer;
import com.taxsettle.entity.IncomeRecord;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.repository.EmployerRepository;
import com.taxsettle.repository.IncomeRecordRepository;
import com.taxsettle.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeRecordService {
    private final IncomeRecordRepository incomeRecordRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final EmployerRepository employerRepository;

    public List<IncomeRecord> findByTaxpayerId(Long taxpayerId) {
        return incomeRecordRepository.findByTaxpayerId(taxpayerId);
    }

    public IncomeRecord create(IncomeRecord record) {
        Taxpayer taxpayer = taxpayerRepository.findById(record.getTaxpayer().getId())
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add income records");
        }
        if (record.getEmployer() != null && record.getEmployer().getId() != null) {
            Employer employer = employerRepository.findById(record.getEmployer().getId())
                    .orElseThrow(() -> new RuntimeException("Employer not found"));
            record.setEmployer(employer);
        }
        return incomeRecordRepository.save(record);
    }

    public List<IncomeRecord> createBatch(Long taxpayerId, List<IncomeRecord> records) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found"));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot add income records");
        }
        records.forEach(r -> {
            r.setTaxpayer(taxpayer);
            if (r.getEmployer() != null && r.getEmployer().getId() != null) {
                Employer employer = employerRepository.findById(r.getEmployer().getId())
                        .orElseThrow(() -> new RuntimeException("Employer not found"));
                r.setEmployer(employer);
            }
        });
        return incomeRecordRepository.saveAll(records);
    }

    public IncomeRecord update(Long id, IncomeRecord updated) {
        IncomeRecord existing = incomeRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income record not found: " + id));
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot modify income records");
        }
        existing.setIncomeType(updated.getIncomeType());
        existing.setMonth(updated.getMonth());
        existing.setGrossIncome(updated.getGrossIncome());
        existing.setPrepaidTax(updated.getPrepaidTax());
        if (updated.getEmployer() != null && updated.getEmployer().getId() != null) {
            Employer employer = employerRepository.findById(updated.getEmployer().getId())
                    .orElseThrow(() -> new RuntimeException("Employer not found"));
            existing.setEmployer(employer);
        } else {
            existing.setEmployer(null);
        }
        return incomeRecordRepository.save(existing);
    }

    public void delete(Long id) {
        IncomeRecord existing = incomeRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income record not found: " + id));
        if (existing.getTaxpayer().getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot delete income records");
        }
        incomeRecordRepository.deleteById(id);
    }

    public void deleteByTaxpayerId(Long taxpayerId) {
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("Taxpayer not found: " + taxpayerId));
        if (taxpayer.getLocked()) {
            throw new IllegalStateException("Taxpayer record is locked, cannot delete income records");
        }
        incomeRecordRepository.deleteByTaxpayerId(taxpayerId);
    }
}
