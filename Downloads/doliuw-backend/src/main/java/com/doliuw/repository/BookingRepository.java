package com.doliuw.repository;

import com.doliuw.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByServiceIdAndBookingDateAndTimeSlot(String serviceId, LocalDate date, String timeSlot);
}
