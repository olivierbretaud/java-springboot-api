package com.formation.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "L'adresse email est invalide.")
    private String email;
}
