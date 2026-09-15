package com.ebike.chatbotModule.service;

import com.ebike.chatbotModule.config.GeminiChatProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiChatClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiChatClient.class);

    private final GeminiChatProperties properties;
    private final RestTemplate restTemplate;
    private final RestTemplate visionRestTemplate;

    public GeminiChatClient(GeminiChatProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
            .build();
        this.visionRestTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(properties.getVisionReadTimeoutMs()))
            .build();
    }

    public boolean isConfigured() {
        return properties.isEnabled() && properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @SuppressWarnings("unchecked")
    public String generateAnswer(String systemInstruction, String prompt) {
        return generateText(systemInstruction, prompt, false);
    }

    public String generateJson(String systemInstruction, String prompt) {
        return generateText(systemInstruction, prompt, true);
    }

    @SuppressWarnings("unchecked")
    private String generateText(String systemInstruction, String prompt, boolean json) {
        if (!isConfigured()) {
            return null;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
            + properties.getModel()
            + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("systemInstruction", Map.of(
            "parts", List.of(Map.of("text", systemInstruction))
        ));
        requestBody.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt))
        )));
        Map<String, Object> generation = new HashMap<>();
        generation.put("temperature", json ? 0.1 : 0.25);
        generation.put("topP", 0.9);
        generation.put("maxOutputTokens", json ? 4096 : 1024);
        if (json) generation.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generation);
        List<Map<String, String>> safetySettings = new ArrayList<>();
        for (String category : List.of(
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT"
        )) {
            safetySettings.add(Map.of("category", category, "threshold", "BLOCK_MEDIUM_AND_ABOVE"));
        }
        requestBody.put("safetySettings", safetySettings);

        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt += 1) {
            try {
                Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(requestBody, headers), Map.class);
                return extractText(response);
            } catch (RestClientException exception) {
                if (attempt >= maxAttempts) {
                    LOGGER.warn("Gemini API call failed after {} attempt(s); chatbot will use fallback response.", attempt, exception);
                    return null;
                }
                LOGGER.warn("Gemini API call attempt {}/{} failed; retrying in {} ms.", attempt, maxAttempts, properties.getRetryDelayMs());
                sleepBeforeRetry();
            }
        }
        return null;
    }

    public String generateWithMedia(String systemInstruction, String prompt, List<MultipartFile> files) {
        if (!isConfigured() || files == null || files.isEmpty()) return null;
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + properties.getModel() + ":generateContent";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.getApiKey());
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        try {
            for (MultipartFile file : files.stream().limit(4).toList()) {
                String mime = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
                parts.add(Map.of("inlineData", Map.of("mimeType", mime, "data", Base64.getEncoder().encodeToString(file.getBytes()))));
            }
        } catch (java.io.IOException exception) {
            LOGGER.warn("Could not read marketplace media for AI analysis", exception);
            return null;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        body.put("contents", List.of(Map.of("role", "user", "parts", parts)));
        body.put("generationConfig", Map.of("temperature", 0.1, "topP", 0.8, "maxOutputTokens", 2048, "responseMimeType", "application/json"));
        try {
            return extractText(visionRestTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class));
        } catch (RestClientException exception) {
            LOGGER.warn("Gemini vision analysis failed; using fallback", exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            return null;
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        Object text = parts.get(0).get("text");
        return text == null ? null : text.toString().trim();
    }

    private void sleepBeforeRetry() {
        long retryDelayMs = Math.max(0, properties.getRetryDelayMs());
        if (retryDelayMs == 0) {
            return;
        }
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
