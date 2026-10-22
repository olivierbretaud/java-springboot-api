package com.formation.core.commandes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.formation.core.commandes.dto.CommandeDto;
import com.formation.core.commandes.dto.CommandeListResponse;
import com.formation.core.commandes.service.ICommandeService;
import com.formation.engine.exceptions.CommandeNonSupprimableException;
import com.formation.core.referentiel.dto.StatutCommandeDto;
import com.formation.security.TestSecurityConfig;
import com.formation.security.UtilisateurDetailsService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.context.annotation.Import;

@WebMvcTest(CommandeController.class)
@SuppressWarnings("null")
@Import(TestSecurityConfig.class)
class CommandeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICommandeService commandeService;

    @MockitoBean
    private UtilisateurDetailsService utilisateurDetailsService;

    @MockitoBean
    private com.formation.security.JwtService jwtService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CommandeDto commandeDto(Long id, String labelStatut) {
        StatutCommandeDto statut = new StatutCommandeDto();
        statut.setId(1L);
        statut.setLabel(labelStatut);

        CommandeDto dto = new CommandeDto();
        dto.setId(id);
        dto.setStatut(statut);
        dto.setMontantTotal(BigDecimal.valueOf(25.98));
        dto.setLignes(List.of());
        return dto;
    }

    // ── POST /commandes ───────────────────────────────────────────────────────

    @Test
    void creerCommande_retourne_201() throws Exception {
        when(commandeService.creerCommande(any(), eq(1L))).thenReturn(commandeDto(1L, "En attente"));

        mockMvc.perform(post("/commandes")
                .param("utilisateurId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lignes\":[{\"idProduit\":1,\"quantite\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.statut.label").value("En attente"));
    }

    @Test
    void creerCommande_retourne_400_si_lignes_vides() throws Exception {
        mockMvc.perform(post("/commandes")
                .param("utilisateurId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lignes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerCommande_retourne_400_si_quantite_invalide() throws Exception {
        mockMvc.perform(post("/commandes")
                .param("utilisateurId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lignes\":[{\"idProduit\":1,\"quantite\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerCommande_retourne_400_si_idProduit_absent() throws Exception {
        mockMvc.perform(post("/commandes")
                .param("utilisateurId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lignes\":[{\"quantite\":2}]}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /commandes/{id} ───────────────────────────────────────────────────

    @Test
    void getCommandeById_retourne_200() throws Exception {
        when(commandeService.getCommandeById(1L)).thenReturn(commandeDto(1L, "En attente"));

        mockMvc.perform(get("/commandes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.statut.label").value("En attente"))
                .andExpect(jsonPath("$.montantTotal").value(25.98));
    }

    @Test
    void getCommandeById_retourne_404_si_inexistant() throws Exception {
        when(commandeService.getCommandeById(99L))
                .thenThrow(new EntityNotFoundException("Commande non trouvée : 99"));

        mockMvc.perform(get("/commandes/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /commandes?utilisateurId= ─────────────────────────────────────────

    @Test
    void listCommandes_retourne_200_avec_wrapper() throws Exception {
        CommandeListResponse response = CommandeListResponse.builder()
                .commandes(List.of(commandeDto(1L, "En attente")))
                .build();
        when(commandeService.listCommandesByUtilisateur(1L)).thenReturn(response);

        mockMvc.perform(get("/commandes").param("utilisateurId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandes").isArray())
                .andExpect(jsonPath("$.commandes[0].id").value(1L));
    }

    @Test
    void listCommandes_retourne_200_avec_liste_vide() throws Exception {
        CommandeListResponse response = CommandeListResponse.builder()
                .commandes(List.of())
                .build();
        when(commandeService.listCommandesByUtilisateur(1L)).thenReturn(response);

        mockMvc.perform(get("/commandes").param("utilisateurId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandes").isEmpty());
    }

    // ── DELETE /commandes/{id} ────────────────────────────────────────────────

    @Test
    void deleteCommande_retourne_204() throws Exception {
        doNothing().when(commandeService).deleteCommande(1L);

        mockMvc.perform(delete("/commandes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCommande_retourne_404_si_inexistant() throws Exception {
        doThrow(new EntityNotFoundException("Commande non trouvée : 99"))
                .when(commandeService).deleteCommande(99L);

        mockMvc.perform(delete("/commandes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCommande_retourne_409_si_statut_non_en_attente() throws Exception {
        doThrow(new CommandeNonSupprimableException(1L, "CONFIRMEE"))
                .when(commandeService).deleteCommande(1L);

        mockMvc.perform(delete("/commandes/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La commande 1 ne peut pas être supprimée (statut : CONFIRMEE)"));
    }

    // ── PATCH /commandes/{id}/statut ──────────────────────────────────────────

    @Test
    void changerStatut_retourne_200() throws Exception {
        when(commandeService.changerStatut(eq(1L), eq(2L))).thenReturn(commandeDto(1L, "Confirmée"));

        mockMvc.perform(patch("/commandes/1/statut")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut.label").value("Confirmée"));
    }

    @Test
    void changerStatut_retourne_400_si_statut_absent() throws Exception {
        mockMvc.perform(patch("/commandes/1/statut")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changerStatut_retourne_404_si_commande_inexistante() throws Exception {
        when(commandeService.changerStatut(eq(99L), any()))
                .thenThrow(new EntityNotFoundException("Commande non trouvée : 99"));

        mockMvc.perform(patch("/commandes/99/statut")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changerStatut_retourne_404_si_statut_inexistant() throws Exception {
        when(commandeService.changerStatut(eq(1L), eq(99L)))
                .thenThrow(new EntityNotFoundException("Statut inconnu : 99"));

        mockMvc.perform(patch("/commandes/1/statut")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statut\":99}"))
                .andExpect(status().isNotFound());
    }
}
