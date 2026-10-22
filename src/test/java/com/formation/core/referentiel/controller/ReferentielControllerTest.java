package com.formation.core.referentiel.controller;

import com.formation.core.referentiel.dto.CategorieDto;
import com.formation.core.referentiel.dto.RoleDto;
import com.formation.core.referentiel.dto.StatutCommandeDto;
import com.formation.core.referentiel.service.IReferentielService;
import com.formation.security.TestSecurityConfig;
import com.formation.security.UtilisateurDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReferentielController.class)
@Import(TestSecurityConfig.class)
class ReferentielControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IReferentielService referentielService;

    @MockitoBean
    private UtilisateurDetailsService utilisateurDetailsService;

    @MockitoBean
    private com.formation.security.JwtService jwtService;

    @Test
    void getReferentiel_retourne_liste_categories() throws Exception {
        CategorieDto cat = new CategorieDto();
        cat.setId(1L);
        cat.setLabel("Fruits");
        when(referentielService.listCategories()).thenReturn(List.of(cat));

        mockMvc.perform(get("/referentiel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].id").value(1))
                .andExpect(jsonPath("$.categories[0].label").value("Fruits"));
    }

    @Test
    void getReferentiel_retourne_liste_vide_si_aucune_categorie() throws Exception {
        when(referentielService.listCategories()).thenReturn(List.of());

        mockMvc.perform(get("/referentiel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isEmpty());
    }

    @Test
    void getReferentiel_retourne_500_si_service_leve_exception() throws Exception {
        when(referentielService.listCategories())
                .thenThrow(new RuntimeException("Erreur base de données"));

        mockMvc.perform(get("/api/referentiel"))
                .andExpect(status().isInternalServerError());
    }

    // ── Rôles ─────────────────────────────────────────────────────────────────

    @Test
    void getReferentiel_retourne_liste_roles() throws Exception {
        RoleDto membre = new RoleDto();
        membre.setId(1L);
        membre.setLabel("Membre");

        RoleDto admin = new RoleDto();
        admin.setId(2L);
        admin.setLabel("Admin");

        when(referentielService.listRoles()).thenReturn(List.of(membre, admin));

        mockMvc.perform(get("/referentiel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0].id").value(1))
                .andExpect(jsonPath("$.roles[0].label").value("Membre"))
                .andExpect(jsonPath("$.roles[1].id").value(2))
                .andExpect(jsonPath("$.roles[1].label").value("Admin"));
    }

    @Test
    void getReferentiel_retourne_liste_vide_si_aucun_role() throws Exception {
        when(referentielService.listRoles()).thenReturn(List.of());

        mockMvc.perform(get("/referentiel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isEmpty());
    }

    // ── StatutCommande ────────────────────────────────────────────────────────

    @Test
    void getReferentiel_retourne_liste_statuts_commande() throws Exception {
        StatutCommandeDto statut = new StatutCommandeDto();
        statut.setId(1L);
        statut.setLabel("En attente");

        when(referentielService.listStatutsCommande()).thenReturn(List.of(statut));

        mockMvc.perform(get("/referentiel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutsCommande[0].id").value(1))
                .andExpect(jsonPath("$.statutsCommande[0].label").value("En attente"));
    }

    @Test
    void getReferentiel_retourne_liste_vide_si_aucun_statut() throws Exception {
        when(referentielService.listStatutsCommande()).thenReturn(List.of());

        mockMvc.perform(get("/referentiel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutsCommande").isEmpty());
    }
}
