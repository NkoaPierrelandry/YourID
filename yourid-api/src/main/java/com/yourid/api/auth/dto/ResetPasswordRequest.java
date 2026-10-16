package com.yourid.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Le jeton est requis.")
    private String token;

    @NotBlank(message = "Le mot de passe est requis.")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres.")
    private String password;
}