package com.formation.core.produits.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.formation.core.produits.dto.ProduitDto;
import com.formation.core.produits.service.IProduitService;
import com.formation.engine.pagination.PageResponse;
import com.formation.security.TestSecurityConfig;
import com.formation.security.UtilisateurDetailsService;

import org.springframework.context.annotation.Import;

@WebMvcTest(ProduitController.class)
@Import(TestSecurityConfig.class)
class ProduitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProduitService produitService;

    @MockitoBean
    private UtilisateurDetailsService utilisateurDetailsService;

    @MockitoBean
    private com.formation.security.JwtService jwtService;

    @Test
    void getProduits_retourne_200_avec_liste() throws Exception {
        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Pomme");
        when(produitService.listProduits()).thenReturn(List.of(dto));

        mockMvc.perform(get("/produit"))
                .andExpect(status().isOk());
    }

    @Test
    void getProduits_retourne_200_avec_liste_vide() throws Exception {
        when(produitService.listProduits()).thenReturn(List.of());

        mockMvc.perform(get("/produit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduit_retourne_201() throws Exception {
        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Café arabica");

        when(produitService.createProduit(any())).thenReturn(dto);

        mockMvc.perform(post("/produit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Café arabica\",\"prix\":12.50,\"idCategorie\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nom").value("Café arabica"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduit_retourne_400_si_nom_vide() throws Exception {
        mockMvc.perform(post("/produit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"\",\"prix\":12.50,\"idCategorie\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduit_retourne_400_si_categorie_absente() throws Exception {
        mockMvc.perform(post("/produit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Café arabica\",\"prix\":12.50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProduitById_retourne_200_avec_dto() throws Exception {
        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Pomme");
        when(produitService.getProduitById(1L)).thenReturn(dto);

        mockMvc.perform(get("/produit/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nom").value("Pomme"));
    }

    @Test
    void getProduitById_retourne_404_si_inexistant() throws Exception {
        when(produitService.getProduitById(99L))
                .thenThrow(new EntityNotFoundException("Produit introuvable : 99"));

        mockMvc.perform(get("/produit/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduit_retourne_204() throws Exception {
        doNothing().when(produitService).deleteProduit(1L);

        mockMvc.perform(delete("/produit/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduit_retourne_404_si_inexistant() throws Exception {
        doThrow(new EntityNotFoundException("Produit introuvable : 99"))
                .when(produitService).deleteProduit(99L);

        mockMvc.perform(delete("/produit/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchProduits_retourne_200_avec_valeurs_par_defaut() throws Exception {
        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Pomme");
        when(produitService.searchProduits(any())).thenReturn(new PageResponse<>(new PageImpl<>(List.of(dto))));

        mockMvc.perform(get("/produit/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].nom").value("Pomme"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void searchProduits_retourne_200_avec_filtre_nom() throws Exception {
        ProduitDto dto = new ProduitDto();
        dto.setId(2L);
        dto.setNom("Café arabica");
        when(produitService.searchProduits(any())).thenReturn(new PageResponse<>(new PageImpl<>(List.of(dto))));

        mockMvc.perform(get("/produit/search")
                .param("search", "café")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].nom").value("Café arabica"));
    }

    @Test
    void searchProduits_retourne_200_avec_liste_vide() throws Exception {
        when(produitService.searchProduits(any())).thenReturn(new PageResponse<>(new PageImpl<>(List.of())));

        mockMvc.perform(get("/produit/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchProduits_retourne_400_si_direction_invalide() throws Exception {
        mockMvc.perform(get("/produit/search")
                .param("direction", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchProduits_retourne_400_si_sort_contient_caracteres_speciaux() throws Exception {
        mockMvc.perform(get("/produit/search")
                .param("sort", "nom; DROP TABLE produits"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchProduits_retourne_400_si_size_depasse_100() throws Exception {
        mockMvc.perform(get("/produit/search")
                .param("size", "200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchProduits_retourne_400_si_page_negative() throws Exception {
        mockMvc.perform(get("/produit/search")
                .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduit_retourne_200_avec_dto_mis_a_jour() throws Exception {
        ProduitDto dto = new ProduitDto();
        dto.setId(1L);
        dto.setNom("Café robusta");

        when(produitService.updateProduit(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/produit/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Café robusta\",\"prix\":14.00,\"idCategorie\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nom").value("Café robusta"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduit_retourne_404_si_produit_inexistant() throws Exception {
        when(produitService.updateProduit(any(), any()))
                .thenThrow(new EntityNotFoundException("Produit introuvable : 99"));

        mockMvc.perform(put("/produit/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Test\",\"prix\":1.00,\"idCategorie\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduit_retourne_400_si_payload_invalide() throws Exception {
        mockMvc.perform(put("/produit/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"\",\"prix\":14.00,\"idCategorie\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MEMBRE")
    void createProduit_retourne_403_si_role_membre() throws Exception {
        mockMvc.perform(post("/produit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Café arabica\",\"prix\":12.50,\"idCategorie\":1}"))
                .andExpect(status().isForbidden());
    }
}
