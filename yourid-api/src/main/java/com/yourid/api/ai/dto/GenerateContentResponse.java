package com.yourid.api.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GenerateContentResponse {
    private String result;
    private String model;
    private Integer tokens;
}
