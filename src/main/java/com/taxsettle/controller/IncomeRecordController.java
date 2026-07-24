package com.taxsettle.controller;

import com.taxsettle.entity.IncomeRecord;
import com.taxsettle.service.IncomeRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taxpayers/{taxpayerId}/incomes")
@RequiredArgsConstructor
public class IncomeRecordController {
    private final IncomeRecordService incomeRecordService;

    @GetMapping
    public List<IncomeRecord> list(@PathVariable Long taxpayerId) {
        return incomeRecordService.findByTaxpayerId(taxpayerId);
    }

    @PostMapping
    public IncomeRecord create(@PathVariable Long taxpayerId, @RequestBody IncomeRecord record) {
        return incomeRecordService.create(record);
    }

    @PostMapping("/batch")
    public List<IncomeRecord> createBatch(@PathVariable Long taxpayerId, @RequestBody List<IncomeRecord> records) {
        return incomeRecordService.createBatch(taxpayerId, records);
    }

    @PutMapping("/{id}")
    public IncomeRecord update(@PathVariable Long taxpayerId, @PathVariable Long id, @RequestBody IncomeRecord record) {
        return incomeRecordService.update(id, record);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long taxpayerId, @PathVariable Long id) {
        incomeRecordService.delete(id);
        return ResponseEntity.ok().build();
    }
}
