package com.formation.core.utilisateurs.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.formation.core.utilisateurs.dto.UtilisateurCreatePayload;
import com.formation.core.utilisateurs.dto.UtilisateurDto;
import com.formation.core.utilisateurs.dto.UtilisateurUpdatePayload;
import com.formation.core.utilisateurs.service.IUtilisateurService;
import com.formation.engine.pagination.PageParams;
import com.formation.engine.pagination.PageResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/utilisateurs")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UtilisateurController {

    private final IUtilisateurService utilisateurService;

    @GetMapping
    public PageResponse<UtilisateurDto> listUtilisateurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {

        PageParams params = new PageParams() {};
        params.setPage(page);
        params.setSize(size);
        params.setSort(sort);
        params.setDirection(direction);

        return utilisateurService.listUtilisateurs(params);
    }

    @GetMapping("/{id}")
    public UtilisateurDto getUtilisateurById(@PathVariable Long id) {
        return utilisateurService.getUtilisateurById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurDto createUtilisateur(@Valid @RequestBody UtilisateurCreatePayload payload) {
        return utilisateurService.createUtilisateur(payload);
    }

    @PutMapping("/{id}")
    public UtilisateurDto updateUtilisateur(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurUpdatePayload payload) {
        return utilisateurService.updateUtilisateur(id, payload);
    }
}
