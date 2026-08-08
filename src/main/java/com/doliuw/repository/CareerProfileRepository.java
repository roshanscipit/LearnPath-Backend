package com.doliuw.repository;

import com.doliuw.entity.CareerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerProfileRepository extends JpaRepository<CareerProfile, Long> {
    Optional<CareerProfile> findByUserId(Long userId);
}
