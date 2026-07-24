package com.taxsettle.controller;

import com.taxsettle.entity.Employer;
import com.taxsettle.service.EmployerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employers")
@RequiredArgsConstructor
public class EmployerController {
    private final EmployerService employerService;

    @GetMapping
    public List<Employer> list() {
        return employerService.findAll();
    }

    @GetMapping("/{id}")
    public Employer get(@PathVariable Long id) {
        return employerService.findById(id);
    }

    @PostMapping
    public Employer create(@RequestBody Employer employer) {
        return employerService.create(employer);
    }

    @PutMapping("/{id}")
    public Employer update(@PathVariable Long id, @RequestBody Employer employer) {
        return employerService.update(id, employer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employerService.delete(id);
        return ResponseEntity.ok().build();
    }
}
