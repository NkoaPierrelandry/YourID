package com.yourid.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "L email est requis.")
    @Email(message = "Format d email invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est requis.")
    private String password;
}
