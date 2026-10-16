package com.yourid.api.withdrawals.controller;

import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.common.security.UserPrincipal;
import com.yourid.api.withdrawals.dto.CreateWithdrawalRequest;
import com.yourid.api.withdrawals.dto.RejectWithdrawalRequest;
import com.yourid.api.withdrawals.dto.WithdrawalResponse;
import com.yourid.api.withdrawals.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping
    public ResponseEntity<ApiResponse<WithdrawalResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWithdrawalRequest request
    ) {
        WithdrawalResponse response = withdrawalService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Demande de retrait creee."));
    }

    @GetMapping("/me")
    public ApiResponse<Page<WithdrawalResponse>> listMine(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return ApiResponse.ok(withdrawalService.listMine(principal.getId(), pageable));
    }

    // Reserve aux administrateurs -- verifie via @PreAuthorize (necessite @EnableMethodSecurity, voir SecurityConfig).
    @PreAuthorize("hasRole(\"ADMIN\")")
    @PatchMapping("/{id}/approve")
    public ApiResponse<WithdrawalResponse> approve(@PathVariable UUID id) {
        return ApiResponse.ok(withdrawalService.approve(id), "Retrait approuve.");
    }

    @PreAuthorize("hasRole(\"ADMIN\")")
    @PatchMapping("/{id}/reject")
    public ApiResponse<WithdrawalResponse> reject(@PathVariable UUID id, @Valid @RequestBody RejectWithdrawalRequest request) {
        return ApiResponse.ok(withdrawalService.reject(id, request), "Retrait rejete.");
    }

    @PreAuthorize("hasRole(\"ADMIN\")")
    @PatchMapping("/{id}/pay")
    public ApiResponse<WithdrawalResponse> markPaid(@PathVariable UUID id) {
        return ApiResponse.ok(withdrawalService.markPaid(id), "Retrait marque comme paye.");
    }
}
