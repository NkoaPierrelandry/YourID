package com.yourid.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "L email est requis.")
    @Email(message = "Format d email invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est requis.")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres.")
    private String password;

    @NotBlank(message = "Le prenom est requis.")
    private String firstName;

    @NotBlank(message = "Le nom est requis.")
    private String lastName;

    @NotBlank(message = "Le nom d utilisateur est requis.")
    @Size(min = 3, max = 50, message = "Le nom d utilisateur doit contenir entre 3 et 50 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Le nom d utilisateur ne peut contenir que lettres, chiffres, points, tirets et underscores.")
    private String username;
}
