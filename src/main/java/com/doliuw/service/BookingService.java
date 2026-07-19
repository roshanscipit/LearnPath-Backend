package com.doliuw.service;

import com.doliuw.dto.AppDtos.*;
import com.doliuw.entity.Booking;
import com.doliuw.entity.User;
import com.doliuw.exception.AppException;
import com.doliuw.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;   // ← injected for email notifications

    @Cacheable(value = "bookings", key = "#userId")
    public List<BookingDto> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toDto).toList();
    }

    @CacheEvict(value = "bookings", key = "#user.id")
    @Transactional
    public BookingDto createBooking(User user, BookingRequest req) {
        // Check slot availability
        boolean slotTaken = bookingRepository.existsByServiceIdAndBookingDateAndTimeSlot(
            req.getServiceId(), req.getBookingDate(), req.getTimeSlot());

        if (slotTaken) {
            throw new AppException("This time slot is already booked. Please choose another.", HttpStatus.CONFLICT);
        }

        Booking booking = Booking.builder()
            .user(user)
            .serviceId(req.getServiceId())
            .serviceName(req.getServiceName())
            .price(req.getPrice())
            .bookingDate(req.getBookingDate())
            .timeSlot(req.getTimeSlot())
            .status(Booking.BookingStatus.CONFIRMED)
            .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created: {} for user {}", booking.getId(), user.getId());

        // ─── Send confirmation emails to user + admin ──────────────
        // Runs asynchronously so it never blocks the HTTP response.
        emailService.sendBookingConfirmation(user, booking);

        return toDto(booking);
    }

    @CacheEvict(value = "bookings", key = "#userId")
    @Transactional
    public BookingDto cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found", HttpStatus.NOT_FOUND));

        if (!booking.getUser().getId().equals(userId)) {
            throw new AppException("Unauthorized", HttpStatus.FORBIDDEN);
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new AppException("Booking already cancelled", HttpStatus.BAD_REQUEST);
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return toDto(booking);
    }

    private BookingDto toDto(Booking b) {
        BookingDto dto = new BookingDto();
        dto.setId(b.getId());
        dto.setServiceId(b.getServiceId());
        dto.setServiceName(b.getServiceName());
        dto.setPrice(b.getPrice());
        dto.setBookingDate(b.getBookingDate());
        dto.setTimeSlot(b.getTimeSlot());
        dto.setStatus(b.getStatus().name());
        dto.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        return dto;
    }
}
