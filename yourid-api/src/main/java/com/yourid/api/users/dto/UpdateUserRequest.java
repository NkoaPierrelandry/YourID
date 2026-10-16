package com.yourid.api.users.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String avatar;

    @Size(max = 1000, message = "La bio ne peut pas depasser 1000 caracteres.")
    private String bio;
}
