package com.formation.core.utilisateurs.mapper;

import com.formation.core.utilisateurs.dto.UtilisateurCreatePayload;
import com.formation.core.utilisateurs.dto.UtilisateurDto;
import com.formation.engine.entities.RoleEntity;
import com.formation.engine.entities.UtilisateurEntity;
import com.formation.engine.utils.EntityMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilisateurMapperTest {

    private final UtilisateurMapper mapper = new UtilisateurMapper(new EntityMapper());

    @Test
    void toUtilisateurDto_mappe_tous_les_champs() {
        UtilisateurEntity entity = entity("alice@test.com", "Dupont", "Jean", "ADMIN");

        UtilisateurDto dto = mapper.toUtilisateurDto(entity);

        assertThat(dto.getEmail()).isEqualTo("alice@test.com");
        assertThat(dto.getNom()).isEqualTo("Dupont");
        assertThat(dto.getPrenom()).isEqualTo("Jean");
        assertThat(dto.getRole()).isNotNull();
        assertThat(dto.getRole().getCode()).isEqualTo("ADMIN");
    }

    @Test
    void toUtilisateurDto_role_null_si_entity_sans_role() {
        UtilisateurEntity entity = new UtilisateurEntity();
        entity.setEmail("bob@test.com");
        entity.setNom("Martin");
        entity.setPrenom("");

        UtilisateurDto dto = mapper.toUtilisateurDto(entity);

        assertThat(dto.getRole()).isNull();
    }

    @Test
    void toEntity_mappe_correctement_avec_prenom() {
        UtilisateurCreatePayload payload = payload("charlie@test.com", "Durand", "Pierre");
        RoleEntity role = role("MEMBRE");

        UtilisateurEntity entity = mapper.toEntity(payload, role, null);

        assertThat(entity.getEmail()).isEqualTo("charlie@test.com");
        assertThat(entity.getNom()).isEqualTo("Durand");
        assertThat(entity.getPrenom()).isEqualTo("Pierre");
        assertThat(entity.getRole()).isSameAs(role);
    }

    @Test
    void toEntity_prenom_vide_si_null() {
        UtilisateurCreatePayload payload = payload("dave@test.com", "Legrand", null);
        RoleEntity role = role("MEMBRE");

        UtilisateurEntity entity = mapper.toEntity(payload, role, null);

        assertThat(entity.getPrenom()).isEqualTo("");
    }

    @Test
    void toEntity_actif_est_false() {
        UtilisateurCreatePayload payload = payload("eve@test.com", "Blanc", "Marie");
        RoleEntity role = role("MEMBRE");

        UtilisateurEntity entity = mapper.toEntity(payload, role, null);

        assertThat(entity.isActif()).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UtilisateurEntity entity(String email, String nom, String prenom, String roleCode) {
        UtilisateurEntity e = new UtilisateurEntity();
        e.setEmail(email);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setRole(role(roleCode));
        return e;
    }

    private RoleEntity role(String code) {
        RoleEntity r = new RoleEntity();
        r.setId(1L);
        r.setCode(code);
        r.setLibelle("Libellé " + code);
        return r;
    }

    private UtilisateurCreatePayload payload(String email, String nom, String prenom) {
        UtilisateurCreatePayload p = new UtilisateurCreatePayload();
        p.setEmail(email);
        p.setNom(nom);
        p.setPrenom(prenom);
        p.setRoleId(1L);
        return p;
    }
}
