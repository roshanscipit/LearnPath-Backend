package com.doliuw.repository;

import com.doliuw.entity.OtpStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpStore, Long> {

    Optional<OtpStore> findTopByMobileAndUsedFalseOrderByCreatedAtDesc(String mobile);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpStore o WHERE o.expiryTime < :now")
    void deleteExpiredOtps(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE OtpStore o SET o.used = true WHERE o.mobile = :mobile AND o.used = false")
    void invalidatePreviousOtps(String mobile);
}
