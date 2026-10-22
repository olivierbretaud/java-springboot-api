package com.formation.core.commandes.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommandeCreatePayload {

    @NotEmpty(message = "La commande doit contenir au moins une ligne")
    @Valid
    private List<LigneCommandePayload> lignes;
}
