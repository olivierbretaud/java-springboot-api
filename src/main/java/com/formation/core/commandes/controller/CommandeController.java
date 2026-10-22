package com.formation.core.commandes.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.formation.core.commandes.dto.ChangerStatutPayload;
import com.formation.core.commandes.dto.CommandeCreatePayload;
import com.formation.core.commandes.dto.CommandeDto;
import com.formation.core.commandes.dto.CommandeListResponse;
import com.formation.core.commandes.service.ICommandeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/commandes")
@RequiredArgsConstructor
@SuppressWarnings("null")
@SecurityRequirement(name = "bearerAuth") // @RequestParam et @PathVariable sont required par défaut → non-null garanti par Spring
public class CommandeController {

    private final ICommandeService commandeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeDto creerCommande(
            @Valid @RequestBody CommandeCreatePayload payload,
            @RequestParam Long utilisateurId) {
        return commandeService.creerCommande(payload, utilisateurId);
    }

    @GetMapping("/{id}")
    public CommandeDto getCommandeById(@PathVariable Long id) {
        return commandeService.getCommandeById(id);
    }

    @GetMapping
    public CommandeListResponse listCommandes(@RequestParam Long utilisateurId) {
        return commandeService.listCommandesByUtilisateur(utilisateurId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommande(@PathVariable Long id) {
        commandeService.deleteCommande(id);
    }

    @PatchMapping("/{id}/statut")
    public CommandeDto changerStatut(
            @PathVariable Long id,
            @Valid @RequestBody ChangerStatutPayload payload) {
        return commandeService.changerStatut(id, payload.getStatut());
    }
}
