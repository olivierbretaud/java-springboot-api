package com.formation.core.utilisateurs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilisateurUpdatePayload {

    @NotBlank
    @Size(max = 100)
    private String nom;

    @Size(max = 100)
    private String prenom;

    @NotNull
    private Long roleId;

    private boolean actif;
}
