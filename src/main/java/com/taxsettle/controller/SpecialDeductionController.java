package com.taxsettle.controller;

import com.taxsettle.entity.SpecialDeduction;
import com.taxsettle.service.SpecialDeductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taxpayers/{taxpayerId}/deductions")
@RequiredArgsConstructor
public class SpecialDeductionController {
    private final SpecialDeductionService specialDeductionService;

    @GetMapping
    public List<SpecialDeduction> list(@PathVariable Long taxpayerId) {
        return specialDeductionService.findByTaxpayerId(taxpayerId);
    }

    @PostMapping
    public SpecialDeduction create(@PathVariable Long taxpayerId, @RequestBody SpecialDeduction deduction) {
        return specialDeductionService.create(taxpayerId, deduction);
    }

    @PostMapping("/batch")
    public List<SpecialDeduction> createBatch(@PathVariable Long taxpayerId, @RequestBody List<SpecialDeduction> deductions) {
        return specialDeductionService.createBatch(taxpayerId, deductions);
    }

    @PutMapping("/{id}")
    public SpecialDeduction update(@PathVariable Long taxpayerId, @PathVariable Long id, @RequestBody SpecialDeduction deduction) {
        return specialDeductionService.update(taxpayerId, id, deduction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long taxpayerId, @PathVariable Long id) {
        specialDeductionService.delete(taxpayerId, id);
        return ResponseEntity.ok().build();
    }
}
