package com.formation.core.referentiel.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReferentielResponse {

    private List<CategorieDto> categories;
    private List<StatutCommandeDto> statutsCommande;
    private List<RoleDto> roles;
}
