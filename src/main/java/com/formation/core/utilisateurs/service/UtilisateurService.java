package com.formation.core.utilisateurs.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

@Service
@RequiredArgsConstructor
public class UtilisateurService implements IUtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final MailService mailService;

    @Override
    public PageResponse<UtilisateurDto> listUtilisateurs(PageParams params) {
        return new PageResponse<>(
            utilisateurRepository.findAll(params.toPageable())
                .map(utilisateurMapper::toUtilisateurDto)
        );
    }

    @Override
    public UtilisateurDto getUtilisateurById(@NonNull Long id) {
        UtilisateurEntity entity = utilisateurRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + id));
        return utilisateurMapper.toUtilisateurDto(entity);
    }

    @Override
    @SuppressWarnings("null")
    public UtilisateurDto createUtilisateur(UtilisateurCreatePayload payload) {
        if (utilisateurRepository.findByEmail(payload.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà : " + payload.getEmail());
        }
        RoleEntity role = roleRepository.findById(payload.getRoleId())
            .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + payload.getRoleId()));

        UtilisateurEntity entity = utilisateurMapper.toEntity(payload, role, null);

        String token = UUID.randomUUID().toString();
        entity.setResetToken(token);
        entity.setResetTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));

        UtilisateurEntity saved = utilisateurRepository.save(entity);
        mailService.envoyerEmailActivation(saved.getEmail(), token);

        return utilisateurMapper.toUtilisateurDto(saved);
    }

    @Override
    @SuppressWarnings("null")
    public UtilisateurDto updateUtilisateur(@NonNull Long id, UtilisateurUpdatePayload payload) {
        UtilisateurEntity entity = utilisateurRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + id));

        RoleEntity role = roleRepository.findById(payload.getRoleId())
            .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + payload.getRoleId()));

        entity.setNom(payload.getNom());
        entity.setPrenom(payload.getPrenom() != null ? payload.getPrenom() : "");
        entity.setRole(role);
        entity.setActif(payload.isActif());

        return utilisateurMapper.toUtilisateurDto(utilisateurRepository.save(entity));
    }
}
