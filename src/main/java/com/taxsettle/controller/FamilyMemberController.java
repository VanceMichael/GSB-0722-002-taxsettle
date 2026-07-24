package com.taxsettle.controller;

import com.taxsettle.entity.FamilyMember;
import com.taxsettle.service.FamilyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-members")
@RequiredArgsConstructor
public class FamilyMemberController {
    private final FamilyMemberService familyMemberService;

    @GetMapping("/taxpayer/{taxpayerId}")
    public List<FamilyMember> listByTaxpayer(@PathVariable Long taxpayerId) {
        return familyMemberService.findByTaxpayerId(taxpayerId);
    }

    @GetMapping("/{id}")
    public FamilyMember get(@PathVariable Long id) {
        return familyMemberService.findById(id);
    }

    @PostMapping("/taxpayer/{taxpayerId}")
    public FamilyMember create(@PathVariable Long taxpayerId, @RequestBody FamilyMember familyMember) {
        return familyMemberService.create(taxpayerId, familyMember);
    }

    @PutMapping("/{id}")
    public FamilyMember update(@PathVariable Long id, @RequestBody FamilyMember familyMember) {
        return familyMemberService.update(id, familyMember);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        familyMemberService.delete(id);
        return ResponseEntity.ok().build();
    }
}
