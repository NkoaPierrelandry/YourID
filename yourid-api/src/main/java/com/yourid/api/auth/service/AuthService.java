package com.yourid.api.auth.service;

import com.yourid.api.auth.dto.AuthResponse;
import com.yourid.api.auth.dto.ForgotPasswordRequest;
import com.yourid.api.auth.dto.LoginRequest;
import com.yourid.api.auth.dto.RegisterRequest;
import com.yourid.api.auth.dto.ResetPasswordRequest;
import com.yourid.api.auth.entity.PasswordResetToken;
import com.yourid.api.auth.repository.PasswordResetTokenRepository;
import com.yourid.api.common.exception.BadRequestException;
import com.yourid.api.common.exception.ConflictException;
import com.yourid.api.common.security.JwtService;
import com.yourid.api.common.security.UserPrincipal;
import com.yourid.api.emails.service.EmailService;
import com.yourid.api.users.entity.User;
import com.yourid.api.users.repository.UserRepository;
import com.yourid.api.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) 
            throw new ConflictException("Cet email est deja utilise.");
        if (userRepository.existsByUsername(request.getUsername())) 
            throw new ConflictException("Ce nom d utilisateur est deja pris.");

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .build();

        user = userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateAccessToken(principal);
        
        return buildAuthResponse(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable apres authentification reussie."));

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateAccessToken(principal);

        return buildAuthResponse(user, token);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password.html?token=" + token;
            emailService.sendPasswordReset(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Lien de reinitialisation invalide."));

        if (resetToken.isUsed()) throw new BadRequestException("Ce lien a deja ete utilise.");
        if (resetToken.isExpired()) throw new BadRequestException("Ce lien a expire.");

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(UUID userId, String currentToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        UserPrincipal principal = new UserPrincipal(user);
        if (!principal.isEnabled() || !principal.isAccountNonLocked() || !principal.isAccountNonExpired()) {
            throw new BadRequestException("Ce compte n'est plus actif.");
        }

        String token = jwtService.refreshAccessToken(principal, currentToken);
        return buildAuthResponse(user, token);
    }
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}