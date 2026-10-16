package com.yourid.api.auth.dto;

import com.yourid.api.users.enums.Role;
import com.yourid.api.users.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private Role role;
    private UserStatus status;
}
