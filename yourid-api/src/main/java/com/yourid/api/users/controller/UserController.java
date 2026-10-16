package com.yourid.api.users.controller;

import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.common.security.UserPrincipal;
import com.yourid.api.users.dto.UpdateUserRequest;
import com.yourid.api.users.dto.UserResponse;
import com.yourid.api.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getById(principal.getId()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.ok(userService.update(principal.getId(), request), "Profil mis a jour.");
    }

    // Profil public consultable par pseudo, ex: page /@username cote frontend.
    @GetMapping("/{username}")
    public ApiResponse<UserResponse> getPublicProfile(@PathVariable String username) {
        return ApiResponse.ok(userService.getByUsername(username));
    }
}
