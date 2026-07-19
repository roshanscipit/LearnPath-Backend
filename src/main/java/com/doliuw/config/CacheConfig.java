package com.doliuw.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    /**
     * Fine-grained Caffeine cache setup.
     * Each cache has its own TTL and max size tuned for its data type.
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(

            // Static reference data – long TTL, small size
            buildCache("companies",    500, 30, TimeUnit.MINUTES),
            buildCache("roles",        200, 60, TimeUnit.MINUTES),
            buildCache("mockTests",    200, 60, TimeUnit.MINUTES),

            // Per-user data – shorter TTL
            buildCache("users",        1000, 10, TimeUnit.MINUTES),
            buildCache("userProgress", 1000, 5,  TimeUnit.MINUTES),

            // OTP store – very short TTL (10 min matches OTP expiry)
            buildCache("otpStore",     500,  10, TimeUnit.MINUTES),

            // Booking slots – medium TTL
            buildCache("bookings",     500,  15, TimeUnit.MINUTES)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, int maxSize, long ttl, TimeUnit unit) {
        return new CaffeineCache(name,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl, unit)
                .recordStats()   // enables /actuator/metrics cache stats
                .build()
        );
    }
}
