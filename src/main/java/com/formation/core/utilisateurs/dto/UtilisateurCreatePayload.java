package com.formation.core.utilisateurs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilisateurCreatePayload {

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String nom;

    @Size(max = 100)
    private String prenom;

    @NotBlank
    @Size(min = 8)
    private String motDePasse;

    @NotNull
    private Long roleId;
}
