package com.doliuw.service;

import com.doliuw.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpCleanupService {

    private final OtpRepository otpRepository;

    /**
     * Runs every 30 minutes to purge expired OTPs from the DB.
     */
    // initialDelay: wait 2 minutes after startup before first run.
    // Without this the job fires while Hibernate (ddl-auto=update) is still
    // creating the otp_store table, causing "Table doesn't exist" errors.
    @Scheduled(initialDelay = 2 * 60 * 1000, fixedRate = 30 * 60 * 1000)
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
        log.debug("Expired OTPs cleaned up");
    }
}
