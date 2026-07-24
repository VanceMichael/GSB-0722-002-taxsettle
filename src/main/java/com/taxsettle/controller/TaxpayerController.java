package com.taxsettle.controller;

import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.SettlementStatus;
import com.taxsettle.service.TaxpayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taxpayers")
@RequiredArgsConstructor
public class TaxpayerController {
    private final TaxpayerService taxpayerService;

    @GetMapping
    public List<Taxpayer> list(
            @RequestParam(required = false) Long employerId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Integer taxYear) {
        if (employerId != null) return taxpayerService.findByEmployerId(employerId);
        if (status != null) return taxpayerService.findByStatus(status);
        if (taxYear != null) return taxpayerService.findByTaxYear(taxYear);
        return taxpayerService.findAll();
    }

    @GetMapping("/{id}")
    public Taxpayer get(@PathVariable Long id) {
        return taxpayerService.findById(id);
    }

    @PostMapping
    public Taxpayer create(@RequestBody Taxpayer taxpayer) {
        return taxpayerService.create(taxpayer);
    }

    @PutMapping("/{id}")
    public Taxpayer update(@PathVariable Long id, @RequestBody Taxpayer taxpayer) {
        return taxpayerService.update(id, taxpayer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taxpayerService.delete(id);
        return ResponseEntity.ok().build();
    }
}
