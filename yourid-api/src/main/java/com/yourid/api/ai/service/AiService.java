package com.yourid.api.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yourid.api.ai.dto.GenerateContentRequest;
import com.yourid.api.ai.dto.GenerateContentResponse;
import com.yourid.api.ai.entity.AiContent;
import com.yourid.api.ai.repository.AiContentRepository;
import com.yourid.api.common.exception.BadRequestException;
import com.yourid.api.users.entity.User;
import com.yourid.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RestClient openAiRestClient;
    private final AiContentRepository aiContentRepository;
    private final UserRepository userRepository;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String model;

    @Transactional
    public GenerateContentResponse generate(UUID userId, GenerateContentRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("La generation IA n est pas configuree (cle OpenAI manquante).");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable."));

        JsonNode response = openAiRestClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                        "model", model,
                        "messages", List.of(Map.of("role", "user", "content", request.getPrompt()))
                ))
                .retrieve()
                .body(JsonNode.class);

        String result = response.path("choices").path(0).path("message").path("content").asText();
        int tokens = response.path("usage").path("total_tokens").asInt(0);

        AiContent content = AiContent.builder()
                .user(user)
                .type(request.getType())
                .prompt(request.getPrompt())
                .result(result)
                .model(model)
                .tokens(tokens)
                .build();
        aiContentRepository.save(content);

        return GenerateContentResponse.builder()
                .result(result)
                .model(model)
                .tokens(tokens)
                .build();
    }
}
