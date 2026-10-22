package com.formation.engine.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.formation.engine.entities.CommandeEntity;

@Repository
public interface CommandeRepository extends JpaRepository<CommandeEntity, Long> {

    List<CommandeEntity> findByUtilisateurId(Long utilisateurId);

    @Query("SELECT c FROM CommandeEntity c " +
           "JOIN FETCH c.lignes l " +
           "JOIN FETCH l.produit " +
           "WHERE c.id = :id")
    Optional<CommandeEntity> findByIdWithLignesAndProduits(@Param("id") Long id);
}
