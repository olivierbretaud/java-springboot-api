package com.formation.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String role;
}
