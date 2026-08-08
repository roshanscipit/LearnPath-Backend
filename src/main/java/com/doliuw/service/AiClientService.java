package com.doliuw.service;

import com.doliuw.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around an OpenAI-compatible /chat/completions endpoint.
 * Kept provider-agnostic on purpose: any OpenAI-compatible gateway
 * (OpenAI, Azure OpenAI, or a local proxy) works by changing
 * ai.openai.base-url / ai.openai.model in application.properties.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiClientService {

    private final RestTemplate restTemplate;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.model}")
    private String model;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    /**
     * Sends a system + user prompt and returns the raw text of the reply.
     * Throws AppException(503) if the key is missing or the call fails.
     */
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(
                "AI Career Agent is not configured. Set the OPENAI_API_KEY environment variable.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "model", model,
            "temperature", 0.4,
            "response_format", Map.of("type", "json_object"),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(body, headers), Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new AppException("AI service returned an empty response.", HttpStatus.BAD_GATEWAY);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AppException("AI service returned no choices.", HttpStatus.BAD_GATEWAY);
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");

        } catch (RestClientException e) {
            log.error("AI provider call failed", e);
            throw new AppException("Could not reach the AI service. Please try again shortly.", HttpStatus.BAD_GATEWAY);
        }
    }
}
