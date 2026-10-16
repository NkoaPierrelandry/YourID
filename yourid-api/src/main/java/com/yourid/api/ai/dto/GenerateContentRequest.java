package com.yourid.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateContentRequest {

    @NotBlank(message = "Le type de contenu est requis (ex: product_description, title).")
    private String type;

    @NotBlank(message = "Le prompt est requis.")
    private String prompt;
}
