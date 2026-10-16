package com.yourid.api.withdrawals.controller;

import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.common.security.UserPrincipal;
import com.yourid.api.withdrawals.dto.PaymentAccountRequest;
import com.yourid.api.withdrawals.dto.PaymentAccountResponse;
import com.yourid.api.withdrawals.service.PaymentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-accounts")
@RequiredArgsConstructor
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentAccountRequest request
    ) {
        PaymentAccountResponse response = paymentAccountService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Compte de paiement ajoute."));
    }

    @GetMapping("/me")
    public ApiResponse<List<PaymentAccountResponse>> listMine(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(paymentAccountService.listMine(principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        paymentAccountService.delete(principal.getId(), id);
        return ApiResponse.ok(null, "Compte supprime.");
    }

    @PatchMapping("/{id}/default")
    public ApiResponse<PaymentAccountResponse> setDefault(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(paymentAccountService.setDefault(principal.getId(), id), "Compte par defaut mis a jour.");
    }
}