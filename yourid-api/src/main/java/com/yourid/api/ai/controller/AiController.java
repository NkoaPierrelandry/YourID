package com.yourid.api.ai.controller;

import com.yourid.api.ai.dto.GenerateContentRequest;
import com.yourid.api.ai.dto.GenerateContentResponse;
import com.yourid.api.ai.service.AiService;
import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    public ApiResponse<GenerateContentResponse> generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateContentRequest request
    ) {
        return ApiResponse.ok(aiService.generate(principal.getId(), request));
    }
}
