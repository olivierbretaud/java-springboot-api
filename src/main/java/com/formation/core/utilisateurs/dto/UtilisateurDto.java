package com.formation.core.utilisateurs.dto;

import java.time.LocalDateTime;

import com.formation.core.referentiel.dto.RoleDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilisateurDto {

    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private RoleDto role;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
