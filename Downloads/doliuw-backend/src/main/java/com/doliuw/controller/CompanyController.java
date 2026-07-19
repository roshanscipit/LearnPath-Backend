package com.doliuw.controller;

import com.doliuw.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // ─── Companies ────────────────────────────────────────────────

    // GET /api/companies
    @GetMapping("/api/companies")
    public ResponseEntity<List<Map<String, Object>>> getAllCompanies(
            @RequestParam(required = false, defaultValue = "all") String category) {
        return ResponseEntity.ok(companyService.getCompaniesByCategory(category));
    }

    // GET /api/companies/{id}
    @GetMapping("/api/companies/{id}")
    public ResponseEntity<?> getCompany(@PathVariable String id) {
        Map<String, Object> company = companyService.getCompanyById(id);
        if (company == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(company);
    }

    // ─── Roles ────────────────────────────────────────────────────

    // GET /api/roles
    @GetMapping("/api/roles")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        return ResponseEntity.ok(companyService.getAllRoles());
    }

    // GET /api/roles/{id}
    @GetMapping("/api/roles/{id}")
    public ResponseEntity<?> getRole(@PathVariable String id) {
        return companyService.getAllRoles().stream()
            .filter(r -> id.equals(r.get("id")))
            .findFirst()
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── Mock Tests ───────────────────────────────────────────────

    // GET /api/mock-tests/list
    @GetMapping("/api/mock-tests/list")
    public ResponseEntity<List<Map<String, Object>>> getMockTests(
            @RequestParam(required = false) String type) {
        List<Map<String, Object>> tests = companyService.getAllMockTests();
        if (type != null) {
            tests = tests.stream().filter(t -> type.equals(t.get("type"))).toList();
        }
        return ResponseEntity.ok(tests);
    }
}
