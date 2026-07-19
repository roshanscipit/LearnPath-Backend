package com.doliuw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync   // ← required for @Async in EmailService (non-blocking email dispatch)
public class DoliuwApplication {
    public static void main(String[] args) {
        SpringApplication.run(DoliuwApplication.class, args);
    }
}
