package com.doliuw.controller;

import com.doliuw.dto.AdminDtos.ComplaintDto;
import com.doliuw.dto.AdminDtos.CreateComplaintRequest;
import com.doliuw.entity.User;
import com.doliuw.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/help")
@RequiredArgsConstructor
public class HelpController {

    private final AdminService adminService;

    // POST /api/help  – user submits a complaint / help request
    @PostMapping
    public ResponseEntity<ComplaintDto> submitComplaint(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateComplaintRequest req) {
        return ResponseEntity.ok(adminService.createComplaint(user, req));
    }

    // GET /api/help/my – user sees their own complaints
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintDto>> myComplaints(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(adminService.getUserComplaints(user.getId()));
    }
}
