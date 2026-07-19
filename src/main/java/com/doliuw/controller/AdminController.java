package com.doliuw.controller;

import com.doliuw.dto.AdminDtos.*;
import com.doliuw.entity.User;
import com.doliuw.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── Dashboard stats ──────────────────────────────────────────

    // GET /api/admin/stats
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats(@AuthenticationPrincipal User admin) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ── Users ────────────────────────────────────────────────────

    // GET /api/admin/users
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers(@AuthenticationPrincipal User admin) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // ── Courses ──────────────────────────────────────────────────

    // GET /api/admin/courses
    @GetMapping("/courses")
    public ResponseEntity<List<CourseDto>> getCourses(@AuthenticationPrincipal User admin) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.getAllCourses());
    }

    // POST /api/admin/courses
    @PostMapping("/courses")
    public ResponseEntity<CourseDto> addCourse(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody CreateCourseRequest req) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.createCourse(req));
    }

    // PUT /api/admin/courses/{id}
    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseDto> updateCourse(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody CreateCourseRequest req) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.updateCourse(id, req));
    }

    // DELETE /api/admin/courses/{id}
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        adminService.requireAdmin(admin);
        adminService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mock Companies ───────────────────────────────────────────

    // GET /api/admin/mock-companies
    @GetMapping("/mock-companies")
    public ResponseEntity<List<MockCompanyDto>> getMockCompanies(@AuthenticationPrincipal User admin) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.getAllMockCompanies());
    }

    // POST /api/admin/mock-companies
    @PostMapping("/mock-companies")
    public ResponseEntity<MockCompanyDto> addMockCompany(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody CreateMockCompanyRequest req) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.createMockCompany(req));
    }

    // DELETE /api/admin/mock-companies/{id}
    @DeleteMapping("/mock-companies/{id}")
    public ResponseEntity<Void> deleteMockCompany(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        adminService.requireAdmin(admin);
        adminService.deleteMockCompany(id);
        return ResponseEntity.noContent().build();
    }

    // ── Bookings / Revenue ───────────────────────────────────────

    // GET /api/admin/bookings
    @GetMapping("/bookings")
    public ResponseEntity<List<AdminBookingDto>> getAllBookings(@AuthenticationPrincipal User admin) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.getAllBookings());
    }

    // ── Help / Complaints ────────────────────────────────────────

    // GET /api/admin/complaints
    @GetMapping("/complaints")
    public ResponseEntity<List<ComplaintDto>> getComplaints(@AuthenticationPrincipal User admin) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.getAllComplaints());
    }

    // PUT /api/admin/complaints/{id}/status
    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintDto> updateComplaintStatus(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody UpdateComplaintStatusRequest req) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(adminService.updateComplaintStatus(id, req.getStatus()));
    }
}
