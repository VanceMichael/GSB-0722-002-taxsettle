package com.taxsettle.service;

import com.taxsettle.entity.Employer;
import com.taxsettle.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployerService {
    private final EmployerRepository employerRepository;

    public List<Employer> findAll() {
        return employerRepository.findAll();
    }

    public Employer findById(Long id) {
        return employerRepository.findById(id).orElseThrow(() -> new RuntimeException("Employer not found: " + id));
    }

    public Employer create(Employer employer) {
        return employerRepository.save(employer);
    }

    public Employer update(Long id, Employer updated) {
        Employer existing = findById(id);
        existing.setName(updated.getName());
        existing.setTaxId(updated.getTaxId());
        existing.setAddress(updated.getAddress());
        existing.setContactPerson(updated.getContactPerson());
        existing.setContactPhone(updated.getContactPhone());
        return employerRepository.save(existing);
    }

    public void delete(Long id) {
        employerRepository.deleteById(id);
    }
}
