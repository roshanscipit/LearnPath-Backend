package com.doliuw.service;

import com.doliuw.dto.CareerAgentDtos.CareerAgentResponse;
import com.doliuw.entity.CareerProfile;
import com.doliuw.entity.User;
import com.doliuw.exception.AppException;
import com.doliuw.repository.CareerProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the "AI Career Agent": takes a role + years of experience
 * (+ optional resume upload), grounds the prompt in this platform's real
 * roadmap topics/features, calls the AI, and returns a structured plan
 * covering suggested level, course recommendations, mock tests, whether
 * to do an AI mock interview, and whether to book a 1:1 session.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareerAgentService {

    private final CareerProfileRepository careerProfileRepository;
    private final RoadmapService roadmapService;
    private final ResumeParsingService resumeParsingService;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CareerAgentResponse analyze(User user, String targetRole, Integer yearsOfExperience,
                                        String additionalContext, MultipartFile resumeFile) {

        if (targetRole == null || targetRole.isBlank()) {
            throw new AppException("targetRole is required.", HttpStatus.BAD_REQUEST);
        }
        if (yearsOfExperience == null || yearsOfExperience < 0) {
            throw new AppException("yearsOfExperience must be a non-negative number.", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> roadmap = roadmapService.getModuleDetail(targetRole);
        if (roadmap == null) {
            throw new AppException("Unknown role: " + targetRole, HttpStatus.NOT_FOUND);
        }

        CareerProfile profile = careerProfileRepository.findByUserId(user.getId())
            .orElse(CareerProfile.builder().user(user).build());

        String resumeText = profile.getResumeText();
        String resumeFileName = profile.getResumeFileName();
        if (resumeFile != null && !resumeFile.isEmpty()) {
            resumeText = resumeParsingService.extractText(resumeFile);
            resumeFileName = resumeFile.getOriginalFilename();
        }

        String userPrompt = buildUserPrompt(user, targetRole, yearsOfExperience, additionalContext, resumeText, roadmap);
        String rawJson = aiClientService.complete(SYSTEM_PROMPT, userPrompt);

        CareerAgentResponse parsed = parseResponse(rawJson, targetRole, yearsOfExperience);

        // Persist inputs + last recommendation so the page can reload without re-calling the AI.
        profile.setTargetRole(targetRole);
        profile.setYearsOfExperience(yearsOfExperience);
        profile.setResumeText(resumeText);
        profile.setResumeFileName(resumeFileName);
        profile.setAdditionalContext(additionalContext);
        try {
            profile.setLastRecommendationJson(objectMapper.writeValueAsString(parsed));
        } catch (Exception e) {
            log.warn("Failed to serialize career agent recommendation for persistence", e);
        }
        careerProfileRepository.save(profile);

        return parsed;
    }

    /** Returns the last saved recommendation for this user, or null if none yet. */
    public CareerAgentResponse getSavedRecommendation(Long userId) {
        return careerProfileRepository.findByUserId(userId)
            .map(CareerProfile::getLastRecommendationJson)
            .filter(json -> json != null && !json.isBlank())
            .map(json -> {
                try {
                    return objectMapper.readValue(json, CareerAgentResponse.class);
                } catch (Exception e) {
                    log.warn("Failed to deserialize saved career agent recommendation", e);
                    return null;
                }
            })
            .orElse(null);
    }

    // ─── Prompting ────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
        You are the AI Career Agent for LearnPath, an IT interview-prep and upskilling platform.
        Given a user's target role, years of experience, optional resume text, and this
        platform's actual curriculum topics (grouped by Basic/Intermediate/Advanced), produce a
        personalized study plan that ONLY recommends features that exist on this platform:
        - Learning Path modules and role roadmap topics (use the exact topic titles given to you)
        - Mock Tests
        - AI Mock Interview (an automated interview simulator)
        - 1:1 Interview session with a human expert (a paid booking feature)

        Respond with STRICT JSON only, no markdown fences, matching exactly this shape:
        {
          "suggestedLevel": "Basic" | "Intermediate" | "Advanced",
          "summary": "2-3 sentence assessment of where this person currently stands",
          "strengths": ["..."],
          "gaps": ["..."],
          "recommendedCourses": [
            {"topicId": "<id from the provided topic list>", "title": "<topic title>", "level": "Basic|Intermediate|Advanced", "reason": "<why, 1 sentence>"}
          ],
          "recommendedMockTests": ["..."],
          "aiInterviewRecommended": true | false,
          "oneOnOneRecommended": true | false,
          "oneOnOneReason": "<1 sentence, empty string if not recommended>",
          "nextSteps": ["3-5 concrete ordered actions"]
        }

        Rules:
        - recommendedCourses must use topicId values taken from the provided topic list — never invent one.
        - Recommend 4-8 courses spanning primarily the suggested level, with 1-2 stretch topics from the next level up.
        - Recommend oneOnOneRecommended=true mainly for users with 3+ years of experience, or anyone targeting
          Advanced roles, or anyone whose resume shows a mismatch worth discussing with a human expert.
        - Keep all text concise and encouraging, never condescending.
        """;

    @SuppressWarnings("unchecked")
    private String buildUserPrompt(User user, String targetRole, Integer years, String additionalContext,
                                    String resumeText, Map<String, Object> roadmap) {

        StringBuilder topicList = new StringBuilder();
        List<Map<String, Object>> levels = (List<Map<String, Object>>) roadmap.get("levels");
        if (levels != null) {
            for (Map<String, Object> level : levels) {
                topicList.append("\n### Level: ").append(level.get("level")).append("\n");
                List<Map<String, Object>> topics = (List<Map<String, Object>>) level.get("topics");
                if (topics != null) {
                    for (Map<String, Object> topic : topics) {
                        topicList.append("- id=").append(topic.get("id"))
                            .append(" | title=").append(topic.get("title"))
                            .append(" | summary=").append(topic.get("summary"))
                            .append("\n");
                    }
                }
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Target role: ").append(roadmap.getOrDefault("moduleName", targetRole)).append(" (id: ").append(targetRole).append(")\n");
        prompt.append("Role variants available: ").append(roadmap.get("variants")).append("\n");
        prompt.append("Years of experience: ").append(years).append("\n");
        if (additionalContext != null && !additionalContext.isBlank()) {
            prompt.append("Additional context from user: ").append(additionalContext).append("\n");
        }
        prompt.append("\nAvailable curriculum topics for this role:\n").append(topicList);

        if (resumeText != null && !resumeText.isBlank()) {
            prompt.append("\n\nResume text (may be messy from PDF/DOCX extraction):\n").append(resumeText);
        } else {
            prompt.append("\n\nNo resume was provided — base the plan on role + years of experience only.");
        }

        return prompt.toString();
    }

    private CareerAgentResponse parseResponse(String rawJson, String targetRole, Integer years) {
        try {
            String cleaned = rawJson == null ? "" : rawJson.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("```\\s*$", "");

            CareerAgentResponse response = objectMapper.readValue(cleaned, CareerAgentResponse.class);
            response.setTargetRole(targetRole);
            response.setYearsOfExperience(years);
            response.setGeneratedAt(LocalDateTime.now().toString());
            return response;
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", rawJson, e);
            throw new AppException("The AI response could not be understood. Please try again.", HttpStatus.BAD_GATEWAY);
        }
    }
}
