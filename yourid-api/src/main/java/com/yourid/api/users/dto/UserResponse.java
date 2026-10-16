package com.yourid.api.users.dto;

import com.yourid.api.users.enums.Role;
import com.yourid.api.users.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private String avatar;
    private String bio;
    private Role role;
    private UserStatus status;
    private boolean emailVerified;
    private Instant createdAt;
}
