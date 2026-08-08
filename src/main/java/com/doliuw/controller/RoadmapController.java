package com.doliuw.controller;

import com.doliuw.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public read-only endpoints for browsing role learning roadmaps:
 * curriculum topics, key points, and interview Q&A per level.
 */
@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    // GET /api/roadmap  -> summary list of all roadmaps
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getIndex() {
        return ResponseEntity.ok(roadmapService.getIndex());
    }

    // GET /api/roadmap/{moduleId}  -> full detail for one role
    @GetMapping("/{moduleId}")
    public ResponseEntity<?> getModuleDetail(@PathVariable String moduleId) {
        Map<String, Object> detail = roadmapService.getModuleDetail(moduleId);
        if (detail == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(detail);
    }

    // GET /api/roadmap/{moduleId}/level/{level}  -> one level (Basic/Intermediate/Advanced)
    @GetMapping("/{moduleId}/level/{level}")
    public ResponseEntity<?> getModuleLevel(@PathVariable String moduleId, @PathVariable String level) {
        Map<String, Object> levelData = roadmapService.getModuleLevel(moduleId, level);
        if (levelData == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(levelData);
    }
}
