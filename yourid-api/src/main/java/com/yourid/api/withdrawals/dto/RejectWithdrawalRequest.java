package com.yourid.api.withdrawals.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectWithdrawalRequest {

    @NotBlank(message = "Un motif de rejet est requis.")
    private String adminNote;
}
