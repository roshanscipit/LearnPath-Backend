package com.doliuw.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves the role-specific learning roadmaps (curriculum, key points, and
 * interview Q&A per level/topic) shipped as static JSON under
 * resources/data/roadmaps/. One file per role (java.json, python.json, ...)
 * plus an index.json summarizing all of them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private static final String BASE_PATH = "data/roadmaps/";

    private final ObjectMapper objectMapper;

    /** Summary of every available roadmap (id, name, variants, levels, topicCount). */
    @Cacheable(value = "roadmap", key = "'index'")
    public List<Map<String, Object>> getIndex() {
        log.debug("Loading roadmap index into cache");
        JsonNode root = readJson("index.json");
        JsonNode modules = root.path("modules");
        return objectMapper.convertValue(modules, List.class);
    }

    /** Full roadmap detail (levels -> topics -> keyPoints/interviewQA) for one role. */
    @Cacheable(value = "roadmap", key = "#moduleId")
    public Map<String, Object> getModuleDetail(String moduleId) {
        String safeId = sanitize(moduleId);
        log.debug("Loading roadmap detail for {}", safeId);
        JsonNode root = readJson(safeId + ".json");
        if (root.isMissingNode()) {
            return null;
        }
        return objectMapper.convertValue(root, LinkedHashMap.class);
    }

    /** Just the topics for one level (Basic / Intermediate / Advanced) of a role. */
    @Cacheable(value = "roadmap", key = "#moduleId + '_' + #level")
    public Map<String, Object> getModuleLevel(String moduleId, String level) {
        Map<String, Object> detail = getModuleDetail(moduleId);
        if (detail == null) return null;

        List<Map<String, Object>> levels = (List<Map<String, Object>>) detail.get("levels");
        if (levels == null) return null;

        return levels.stream()
            .filter(l -> level.equalsIgnoreCase(String.valueOf(l.get("level"))))
            .findFirst()
            .orElse(null);
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private JsonNode readJson(String fileName) {
        try (InputStream is = new ClassPathResource(BASE_PATH + fileName).getInputStream()) {
            return objectMapper.readTree(is);
        } catch (IOException e) {
            log.warn("Roadmap data file not found: {}", fileName);
            return objectMapper.missingNode();
        }
    }

    /** Prevent path traversal via the moduleId path variable. */
    private String sanitize(String moduleId) {
        return moduleId == null ? "" : moduleId.replaceAll("[^a-zA-Z0-9\\-]", "");
    }
}
