package com.doliuw.service;

import com.doliuw.dto.AppDtos.*;
import com.doliuw.entity.User;
import com.doliuw.entity.UserProgress;
import com.doliuw.exception.AppException;
import com.doliuw.repository.UserProgressRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProgressService {

    private final UserProgressRepository progressRepository;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "userProgress", key = "#userId")
    public UserProgressDto getProgress(Long userId) {
        UserProgress progress = progressRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException("Progress not found", HttpStatus.NOT_FOUND));
        return toDto(progress);
    }

    @CachePut(value = "userProgress", key = "#userId")
    @Transactional
    public UserProgressDto updateProgress(Long userId, UpdateProgressRequest req) {
        UserProgress progress = progressRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException("Progress not found", HttpStatus.NOT_FOUND));

        progress.setSelectedRole(req.getSelectedRole());
        progress.setSelectedVariant(req.getSelectedVariant());
        progress.setCurrentModule(req.getCurrentModule());
        progress.setOverallProgress(req.getOverallProgress());
        progress.setTestsTaken(req.getTestsTaken());
        progress.setAverageScore(req.getAverageScore());

        // Serialize list → JSON string
        try {
            List<String> modules = req.getCompletedModules() != null ? req.getCompletedModules() : new ArrayList<>();
            progress.setCompletedModules(objectMapper.writeValueAsString(modules));
        } catch (Exception e) {
            progress.setCompletedModules("[]");
        }

        progressRepository.save(progress);
        return toDto(progress);
    }

    @CacheEvict(value = "userProgress", key = "#userId")
    public void evictCache(Long userId) { }

    private UserProgressDto toDto(UserProgress p) {
        UserProgressDto dto = new UserProgressDto();
        dto.setSelectedRole(p.getSelectedRole());
        dto.setSelectedVariant(p.getSelectedVariant());
        dto.setCurrentModule(p.getCurrentModule());
        dto.setOverallProgress(p.getOverallProgress());
        dto.setTestsTaken(p.getTestsTaken());
        dto.setAverageScore(p.getAverageScore());

        try {
            List<String> modules = objectMapper.readValue(
                p.getCompletedModules(), new TypeReference<>() {});
            dto.setCompletedModules(modules);
        } catch (Exception e) {
            dto.setCompletedModules(new ArrayList<>());
        }
        return dto;
    }
}
