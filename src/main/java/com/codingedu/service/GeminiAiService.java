package com.codingedu.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiAiService {

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final List<String> models;

    public GeminiAiService(ObjectMapper objectMapper,
                           @Value("${gemini.api.key:${GEMINI_API_KEY:}}") String apiKey,
                           @Value("${gemini.api.model:}") String model,
                           @Value("${gemini.api.models:}") String models) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.models = resolveModels(model, models);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public Optional<String> generateText(String systemInstruction, String userPrompt) {
        if (!isConfigured() || userPrompt == null || userPrompt.isBlank()) {
            return Optional.empty();
        }

        String requestBody;
        try {
            requestBody = buildRequestBody(systemInstruction, userPrompt);
        } catch (JacksonException e) {
            return Optional.empty();
        }

        for (String model : models) {
            Optional<String> generated = generateTextWithModel(model, requestBody);
            if (generated.isPresent()) {
                return generated;
            }
        }
        return Optional.empty();
    }

    private Optional<String> generateTextWithModel(String model, String requestBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(GEMINI_ENDPOINT.formatted(model)))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return extractText(response.body()).filter(text -> !text.isBlank());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private String buildRequestBody(String systemInstruction, String userPrompt) throws JacksonException {
        String prompt = """
                [역할 지시]
                %s

                [사용자 요청]
                %s
                """.formatted(systemInstruction, userPrompt);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                ))
        );
        return objectMapper.writeValueAsString(body);
    }

    private Optional<String> extractText(String responseBody) throws JacksonException {
        GeminiResponse response = objectMapper.readValue(responseBody, GeminiResponse.class);
        if (response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }
        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null) {
            return Optional.empty();
        }
        return candidate.content().parts().stream()
                .map(Part::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst();
    }

    private List<String> resolveModels(String primaryModel, String configuredModels) {
        LinkedHashSet<String> orderedModels = new LinkedHashSet<>();
        addModels(orderedModels, primaryModel);
        addModels(orderedModels, configuredModels);
        addModels(orderedModels, "gemini-2.5-flash,gemini-2.5-flash-lite,gemini-2.0-flash,gemini-2.0-flash-lite");
        return List.copyOf(orderedModels);
    }

    private void addModels(LinkedHashSet<String> orderedModels, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Arrays.stream(value.split(","))
                .map(this::normalizeModel)
                .filter(model -> !model.isBlank())
                .forEach(orderedModels::add);
    }

    private String normalizeModel(String model) {
        String value = model == null ? "" : model.trim();
        return value.startsWith("models/") ? value.substring("models/".length()) : value;
    }

    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}
}

