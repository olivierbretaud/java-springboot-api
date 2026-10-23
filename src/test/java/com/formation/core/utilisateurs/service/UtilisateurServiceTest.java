package com.formation.core.utilisateurs.service;

import com.formation.core.auth.service.MailService;
import com.formation.core.utilisateurs.dto.UtilisateurCreatePayload;
import com.formation.core.utilisateurs.dto.UtilisateurDto;
import com.formation.core.utilisateurs.dto.UtilisateurUpdatePayload;
import com.formation.core.utilisateurs.mapper.UtilisateurMapper;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.pagination.PageParams;
import com.formation.engine.pagination.PageResponse;
import com.formation.engine.repositories.RoleRepository;
import com.formation.engine.repositories.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UtilisateurMapper utilisateurMapper;
    @Mock private MailService mailService;

    @InjectMocks private UtilisateurService utilisateurService;

    // ── listUtilisateurs ──────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void listUtilisateurs_retourne_page_mappee() {
        UtilisateurEntity entity = entityUtilisateur("alice@test.com");
        UtilisateurDto dto = dto("alice@test.com");

        when(utilisateurRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(utilisateurMapper.toUtilisateurDto(entity)).thenReturn(dto);

        PageResponse<UtilisateurDto> result = utilisateurService.listUtilisateurs(params());

        assertThat(result.getContenu()).hasSize(1);
        assertThat(result.getContenu().get(0).getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @SuppressWarnings("null")
    void listUtilisateurs_retourne_page_vide() {
        when(utilisateurRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<UtilisateurDto> result = utilisateurService.listUtilisateurs(params());

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContenu()).isEmpty();
    }

    // ── getUtilisateurById ────────────────────────────────────────────────────

    @Test
    void getUtilisateurById_retourne_dto_si_trouve() {
        UtilisateurEntity entity = entityUtilisateur("bob@test.com");
        UtilisateurDto dto = dto("bob@test.com");

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(utilisateurMapper.toUtilisateurDto(entity)).thenReturn(dto);

        UtilisateurDto result = utilisateurService.getUtilisateurById(1L);

        assertThat(result.getEmail()).isEqualTo("bob@test.com");
    }

    @Test
    void getUtilisateurById_leve_exception_si_inexistant() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> utilisateurService.getUtilisateurById(99L));
    }

    // ── createUtilisateur ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void createUtilisateur_retourne_dto_cree() {
        UtilisateurCreatePayload payload = createPayload("charlie@test.com", 1L);
        UtilisateurEntity entity = entityUtilisateur("charlie@test.com");
        UtilisateurDto dto = dto("charlie@test.com");

        when(utilisateurRepository.findByEmail("charlie@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L)));
        when(utilisateurMapper.toEntity(any(), any(), any())).thenReturn(entity);
        when(utilisateurRepository.save(any())).thenReturn(entity);
        when(utilisateurMapper.toUtilisateurDto(entity)).thenReturn(dto);

        UtilisateurDto result = utilisateurService.createUtilisateur(payload);

        assertThat(result.getEmail()).isEqualTo("charlie@test.com");
        verify(utilisateurRepository).save(any());
    }

    @Test
    void createUtilisateur_leve_exception_si_email_deja_utilise() {
        UtilisateurCreatePayload payload = createPayload("existing@test.com", 1L);
        when(utilisateurRepository.findByEmail("existing@test.com"))
                .thenReturn(Optional.of(entityUtilisateur("existing@test.com")));

        assertThrows(IllegalArgumentException.class,
                () -> utilisateurService.createUtilisateur(payload));
    }

    @Test
    void createUtilisateur_leve_exception_si_role_inexistant() {
        UtilisateurCreatePayload payload = createPayload("new@test.com", 99L);
        when(utilisateurRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> utilisateurService.createUtilisateur(payload));
    }

    @Test
    @SuppressWarnings("null")
    void createUtilisateur_envoie_email_activation() {
        UtilisateurCreatePayload payload = createPayload("dave@test.com", 1L);
        UtilisateurEntity entity = entityUtilisateur("dave@test.com");

        when(utilisateurRepository.findByEmail("dave@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L)));
        when(utilisateurMapper.toEntity(any(), any(), any())).thenReturn(entity);
        when(utilisateurRepository.save(any())).thenReturn(entity);
        when(utilisateurMapper.toUtilisateurDto(entity)).thenReturn(dto("dave@test.com"));

        utilisateurService.createUtilisateur(payload);

        verify(mailService).envoyerEmailActivation(eq("dave@test.com"), any());
    }

    // ── updateUtilisateur ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("null")
    void updateUtilisateur_retourne_dto_mis_a_jour() {
        UtilisateurUpdatePayload payload = updatePayload("Dupont", 1L);
        UtilisateurEntity entity = entityUtilisateur("alice@test.com");
        UtilisateurDto dto = dto("alice@test.com");

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L)));
        when(utilisateurRepository.save(any(UtilisateurEntity.class))).thenReturn(entity);
        when(utilisateurMapper.toUtilisateurDto(entity)).thenReturn(dto);

        UtilisateurDto result = utilisateurService.updateUtilisateur(1L, payload);

        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        verify(utilisateurRepository).save(entity);
    }

    @Test
    void updateUtilisateur_leve_exception_si_utilisateur_inexistant() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());
        UtilisateurUpdatePayload payload = updatePayload("Test", 1L);

        assertThrows(EntityNotFoundException.class,
                () -> utilisateurService.updateUtilisateur(99L, payload));
    }

    @Test
    void updateUtilisateur_leve_exception_si_role_inexistant() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(entityUtilisateur("alice@test.com")));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());
        UtilisateurUpdatePayload payload = updatePayload("Test", 99L);

        assertThrows(EntityNotFoundException.class,
                () -> utilisateurService.updateUtilisateur(1L, payload));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UtilisateurEntity entityUtilisateur(String email) {
        UtilisateurEntity e = new UtilisateurEntity();
        e.setEmail(email);
        e.setNom("Test");
        e.setPrenom("User");
        RoleEntity r = new RoleEntity();
        r.setCode("ADMIN");
        e.setRole(r);
        return e;
    }

    private RoleEntity role(Long id) {
        RoleEntity r = new RoleEntity();
        r.setId(id);
        r.setCode("ADMIN");
        return r;
    }

    private UtilisateurDto dto(String email) {
        UtilisateurDto dto = new UtilisateurDto();
        dto.setEmail(email);
        return dto;
    }

    private PageParams params() {
        return new PageParams() {};
    }

    private UtilisateurCreatePayload createPayload(String email, Long roleId) {
        UtilisateurCreatePayload p = new UtilisateurCreatePayload();
        p.setEmail(email);
        p.setNom("Test");
        p.setRoleId(roleId);
        return p;
    }

    private UtilisateurUpdatePayload updatePayload(String nom, Long roleId) {
        UtilisateurUpdatePayload p = new UtilisateurUpdatePayload();
        p.setNom(nom);
        p.setRoleId(roleId);
        return p;
    }
}
