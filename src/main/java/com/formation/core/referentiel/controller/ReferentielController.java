package com.formation.core.referentiel.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.formation.core.referentiel.dto.ReferentielResponse;
import com.formation.core.referentiel.service.IReferentielService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/referentiel")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReferentielController {

    private final IReferentielService referentielService;

    @GetMapping
    public ReferentielResponse getReferentiel() {
        return ReferentielResponse.builder()
                .categories(referentielService.listCategories())
                .statutsCommande(referentielService.listStatutsCommande())
                .roles(referentielService.listRoles())
                .build();
    }
}
