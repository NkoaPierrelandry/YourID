package com.yourid.api.auth.controller;

import com.yourid.api.auth.dto.AuthResponse;
import com.yourid.api.auth.dto.LoginRequest;
import com.yourid.api.auth.dto.RegisterRequest;
import com.yourid.api.auth.dto.ResetPasswordRequest;
import com.yourid.api.auth.service.AuthService;
import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.auth.dto.ForgotPasswordRequest;
import com.yourid.api.common.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Authorization") String authorization
    ) {
        String currentToken = authorization.substring(7);
        return ApiResponse.ok(authService.refresh(principal.getId(), currentToken));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Compte cree avec succes."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Connexion reussie."));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok(null, "Si cet email existe, un lien a ete envoye.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(null, "Mot de passe reinitialise");
    }
}
