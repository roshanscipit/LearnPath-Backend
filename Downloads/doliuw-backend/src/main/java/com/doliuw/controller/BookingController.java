package com.doliuw.controller;

import com.doliuw.dto.AppDtos.*;
import com.doliuw.entity.User;
import com.doliuw.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // GET /api/bookings  – list all bookings for the logged-in user
    @GetMapping
    public ResponseEntity<List<BookingDto>> getMyBookings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.getUserBookings(user.getId()));
    }

    // POST /api/bookings  – create a new booking
    @PostMapping
    public ResponseEntity<BookingDto> createBooking(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookingRequest req) {
        return ResponseEntity.ok(bookingService.createBooking(user, req));
    }

    // DELETE /api/bookings/{id}  – cancel a booking
    @DeleteMapping("/{id}")
    public ResponseEntity<BookingDto> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, user.getId()));
    }
}
