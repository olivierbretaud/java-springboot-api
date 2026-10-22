package com.formation.engine.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.formation.engine.entities.UtilisateurEntity;

public interface UtilisateurRepository extends JpaRepository<UtilisateurEntity, Long> {
    Optional<UtilisateurEntity> findByEmail(String email);

    Optional<UtilisateurEntity> findByResetToken(String token);
}
 