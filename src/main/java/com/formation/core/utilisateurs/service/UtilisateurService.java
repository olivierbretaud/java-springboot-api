package com.formation.core.utilisateurs.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
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

@Service
@RequiredArgsConstructor
public class UtilisateurService implements IUtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
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
    public UtilisateurDto getUtilisateurById(Long id) {
        UtilisateurEntity entity = utilisateurRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + id));
        return utilisateurMapper.toUtilisateurDto(entity);
    }

    @Override
    public UtilisateurDto createUtilisateur(UtilisateurCreatePayload payload) {
        if (utilisateurRepository.findByEmail(payload.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà : " + payload.getEmail());
        }
        RoleEntity role = roleRepository.findById(payload.getRoleId())
            .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + payload.getRoleId()));

        String motDePasseHache = passwordEncoder.encode(payload.getMotDePasse());
        UtilisateurEntity entity = utilisateurMapper.toEntity(payload, role, motDePasseHache);

        String token = UUID.randomUUID().toString();
        entity.setResetToken(token);
        entity.setResetTokenExpiry(LocalDateTime.now().plusHours(24));

        UtilisateurEntity saved = utilisateurRepository.save(entity);
        mailService.envoyerEmailActivation(saved.getEmail(), token);

        return utilisateurMapper.toUtilisateurDto(saved);
    }

    @Override
    public UtilisateurDto updateUtilisateur(Long id, UtilisateurUpdatePayload payload) {
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
