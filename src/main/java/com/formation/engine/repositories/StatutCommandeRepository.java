package com.formation.engine.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.formation.engine.entities.StatutCommandeEntity;

@Repository
public interface StatutCommandeRepository extends JpaRepository<StatutCommandeEntity, Long> {

    Optional<StatutCommandeEntity> findByCode(String code);
}
