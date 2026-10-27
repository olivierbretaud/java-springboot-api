package com.formation.core.utilisateurs.controller;

import com.formation.core.utilisateurs.dto.UtilisateurDto;
import com.formation.core.utilisateurs.service.IUtilisateurService;
import com.formation.engine.pagination.PageResponse;
import com.formation.security.JwtService;
import com.formation.security.TestSecurityConfig;
import com.formation.security.UtilisateurDetailsService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UtilisateurController.class)
@Import(TestSecurityConfig.class)
class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUtilisateurService utilisateurService;

    @MockitoBean
    private UtilisateurDetailsService utilisateurDetailsService;

    @MockitoBean
    private JwtService jwtService;

    // ── GET /admin/utilisateurs ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUtilisateurs_retourne_200_avec_page() throws Exception {
        when(utilisateurService.listUtilisateurs(any()))
                .thenReturn(new PageResponse<>(new PageImpl<>(List.of(dto(1L, "alice@test.com")))));

        mockMvc.perform(get("/admin/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].email").value("alice@test.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUtilisateurs_retourne_200_avec_page_vide() throws Exception {
        when(utilisateurService.listUtilisateurs(any()))
                .thenReturn(new PageResponse<>(new PageImpl<>(List.of())));

        mockMvc.perform(get("/admin/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "MEMBRE")
    void listUtilisateurs_retourne_403_si_role_membre() throws Exception {
        mockMvc.perform(get("/admin/utilisateurs"))
                .andExpect(status().isForbidden());
    }

    // ── GET /admin/utilisateurs/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUtilisateurById_retourne_200_avec_dto() throws Exception {
        when(utilisateurService.getUtilisateurById(1L)).thenReturn(dto(1L, "bob@test.com"));

        mockMvc.perform(get("/admin/utilisateurs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("bob@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUtilisateurById_retourne_404_si_inexistant() throws Exception {
        when(utilisateurService.getUtilisateurById(99L))
                .thenThrow(new EntityNotFoundException("Utilisateur introuvable : 99"));

        mockMvc.perform(get("/admin/utilisateurs/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /admin/utilisateurs ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUtilisateur_retourne_201_avec_dto() throws Exception {
        when(utilisateurService.createUtilisateur(any())).thenReturn(dto(2L, "charlie@test.com"));

        mockMvc.perform(post("/admin/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"charlie@test.com\",\"nom\":\"Martin\",\"roleId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("charlie@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUtilisateur_retourne_400_si_email_manquant() throws Exception {
        mockMvc.perform(post("/admin/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Martin\",\"roleId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUtilisateur_retourne_400_si_email_invalide() throws Exception {
        mockMvc.perform(post("/admin/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pas-un-email\",\"nom\":\"Martin\",\"roleId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUtilisateur_retourne_400_si_nom_manquant() throws Exception {
        mockMvc.perform(post("/admin/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"roleId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUtilisateur_retourne_400_si_roleId_manquant() throws Exception {
        mockMvc.perform(post("/admin/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"nom\":\"Martin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MEMBRE")
    void createUtilisateur_retourne_403_si_role_membre() throws Exception {
        mockMvc.perform(post("/admin/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"nom\":\"Martin\",\"roleId\":1}"))
                .andExpect(status().isForbidden());
    }

    // ── PUT /admin/utilisateurs/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUtilisateur_retourne_200_avec_dto() throws Exception {
        when(utilisateurService.updateUtilisateur(eq(1L), any())).thenReturn(dto(1L, "alice@test.com"));

        mockMvc.perform(put("/admin/utilisateurs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Dupont\",\"roleId\":1,\"actif\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUtilisateur_retourne_404_si_inexistant() throws Exception {
        when(utilisateurService.updateUtilisateur(eq(99L), any()))
                .thenThrow(new EntityNotFoundException("Utilisateur introuvable : 99"));

        mockMvc.perform(put("/admin/utilisateurs/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Dupont\",\"roleId\":1,\"actif\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUtilisateur_retourne_400_si_nom_vide() throws Exception {
        mockMvc.perform(put("/admin/utilisateurs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"\",\"roleId\":1,\"actif\":true}"))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UtilisateurDto dto(Long id, String email) {
        UtilisateurDto dto = new UtilisateurDto();
        dto.setId(id);
        dto.setEmail(email);
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        return dto;
    }
}
