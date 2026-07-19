package com.doliuw.service;

import com.doliuw.dto.AdminDtos.*;
import com.doliuw.entity.*;
import com.doliuw.exception.AppException;
import com.doliuw.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository         userRepository;
    private final BookingRepository      bookingRepository;
    private final ComplaintRepository    complaintRepository;
    private final CourseRepository       courseRepository;
    private final MockCompanyRepository  mockCompanyRepository;
    private final com.doliuw.repository.QuestionRepository questionRepository;

    // ── Guard ──────────────────────────────────────────────────────

    /**
     * Check if the requesting user is an admin.
     * Admin = email ends with @doliuw.admin  OR  id == 1 (first registered user).
     * Adjust this logic as needed for your production setup.
     */
    public void requireAdmin(User user) {
        if (user == null) {
            throw new AppException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = (user.getId() != null && user.getId() == 1L)
            || (user.getEmail() != null && user.getEmail().endsWith("@doliuw.admin"));
        if (!isAdmin) {
            throw new AppException("Admin access required", HttpStatus.FORBIDDEN);
        }
    }

    // ── Dashboard Stats ────────────────────────────────────────────

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalBookings(bookingRepository.count());
        stats.setTotalRevenue(
            bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .mapToLong(Booking::getPrice)
                .sum()
        );
        stats.setOpenComplaints(complaintRepository.countByStatus("OPEN"));
        stats.setTotalCourses(courseRepository.count());
        stats.setTotalMockCompanies(mockCompanyRepository.count());
        stats.setTotalQuestions(questionRepository.count());
        stats.setTotalCodingQuestions(questionRepository.countByQuestionTypeAndActiveTrue("CODING"));
        stats.setTotalAptitudeQuestions(questionRepository.countByQuestionTypeAndActiveTrue("APTITUDE"));
        stats.setTotalSystemDesignQuestions(questionRepository.countByQuestionTypeAndActiveTrue("SYSTEM_DESIGN"));
        return stats;
    }

    // ── Users ──────────────────────────────────────────────────────

    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserDto).toList();
    }

    // ── Courses ────────────────────────────────────────────────────

    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream().map(this::toCourseDto).toList();
    }

    @Transactional
    public CourseDto createCourse(CreateCourseRequest req) {
        Course course = Course.builder()
            .title(req.getTitle())
            .description(req.getDescription())
            .category(req.getCategory())
            .price(req.getPrice())
            .active(req.isActive())
            .build();
        return toCourseDto(courseRepository.save(course));
    }

    @Transactional
    public CourseDto updateCourse(Long id, CreateCourseRequest req) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new AppException("Course not found", HttpStatus.NOT_FOUND));
        course.setTitle(req.getTitle());
        course.setDescription(req.getDescription());
        course.setCategory(req.getCategory());
        course.setPrice(req.getPrice());
        course.setActive(req.isActive());
        return toCourseDto(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id))
            throw new AppException("Course not found", HttpStatus.NOT_FOUND);
        courseRepository.deleteById(id);
    }

    // ── Mock Companies ─────────────────────────────────────────────

    public List<MockCompanyDto> getAllMockCompanies() {
        return mockCompanyRepository.findAll().stream().map(this::toMockCompanyDto).toList();
    }

    @Transactional
    public MockCompanyDto createMockCompany(CreateMockCompanyRequest req) {
        MockCompany mc = MockCompany.builder()
            .name(req.getName())
            .category(req.getCategory())
            .logoUrl(req.getLogoUrl())
            .difficulty(req.getDifficulty())
            .build();
        return toMockCompanyDto(mockCompanyRepository.save(mc));
    }

    @Transactional
    public void deleteMockCompany(Long id) {
        if (!mockCompanyRepository.existsById(id))
            throw new AppException("Mock company not found", HttpStatus.NOT_FOUND);
        mockCompanyRepository.deleteById(id);
    }

    // ── Bookings (admin view) ──────────────────────────────────────

    public List<AdminBookingDto> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::toAdminBookingDto).toList();
    }

    // ── Complaints ─────────────────────────────────────────────────

    public List<ComplaintDto> getAllComplaints() {
        return complaintRepository.findAll().stream().map(this::toComplaintDto).toList();
    }

    public List<ComplaintDto> getUserComplaints(Long userId) {
        return complaintRepository.findByUserId(userId).stream().map(this::toComplaintDto).toList();
    }

    @Transactional
    public ComplaintDto createComplaint(User user, CreateComplaintRequest req) {
        Complaint c = Complaint.builder()
            .user(user)
            .subject(req.getSubject())
            .message(req.getMessage())
            .status("OPEN")
            .build();
        return toComplaintDto(complaintRepository.save(c));
    }

    @Transactional
    public ComplaintDto updateComplaintStatus(Long id, String status) {
        Complaint c = complaintRepository.findById(id)
            .orElseThrow(() -> new AppException("Complaint not found", HttpStatus.NOT_FOUND));
        c.setStatus(status.toUpperCase());
        return toComplaintDto(complaintRepository.save(c));
    }

    // ── Mappers ────────────────────────────────────────────────────

    private AdminUserDto toUserDto(User u) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(u.getId());
        dto.setName(u.getName());
        dto.setEmail(u.getEmail());
        dto.setMobile(u.getMobile());
        dto.setProvider(u.getProvider().name());
        dto.setEnabled(u.isEnabled());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }

    private CourseDto toCourseDto(Course c) {
        CourseDto dto = new CourseDto();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setCategory(c.getCategory());
        dto.setPrice(c.getPrice());
        dto.setActive(c.isActive());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private MockCompanyDto toMockCompanyDto(MockCompany mc) {
        MockCompanyDto dto = new MockCompanyDto();
        dto.setId(mc.getId());
        dto.setName(mc.getName());
        dto.setCategory(mc.getCategory());
        dto.setLogoUrl(mc.getLogoUrl());
        dto.setDifficulty(mc.getDifficulty());
        dto.setCreatedAt(mc.getCreatedAt());
        return dto;
    }

    private AdminBookingDto toAdminBookingDto(Booking b) {
        AdminBookingDto dto = new AdminBookingDto();
        dto.setId(b.getId());
        dto.setUserName(b.getUser() != null ? b.getUser().getName() : "");
        dto.setUserEmail(b.getUser() != null ? b.getUser().getEmail() : "");
        dto.setServiceName(b.getServiceName());
        dto.setPrice(b.getPrice());
        dto.setBookingDate(b.getBookingDate());
        dto.setTimeSlot(b.getTimeSlot());
        dto.setStatus(b.getStatus().name());
        dto.setCreatedAt(b.getCreatedAt());
        return dto;
    }

    private ComplaintDto toComplaintDto(Complaint c) {
        ComplaintDto dto = new ComplaintDto();
        dto.setId(c.getId());
        dto.setUserName(c.getUser() != null ? c.getUser().getName() : "");
        dto.setUserEmail(c.getUser() != null ? c.getUser().getEmail() : "");
        dto.setSubject(c.getSubject());
        dto.setMessage(c.getMessage());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}
