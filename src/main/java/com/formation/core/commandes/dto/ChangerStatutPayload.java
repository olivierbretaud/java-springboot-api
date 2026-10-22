package com.formation.core.commandes.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangerStatutPayload {

    @NotNull
    private Long statut;
}
