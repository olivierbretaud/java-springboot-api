package com.formation.engine.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "commandes")
@EntityListeners(AuditingEntityListener.class)
public class CommandeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime date_creation;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime date_modification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_id", nullable = false)
    private StatutCommandeEntity statut;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private UtilisateurEntity utilisateur;

    // mappedBy = nom du champ dans LigneCommandeEntity qui porte le @JoinColumn
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommandeEntity> lignes = new ArrayList<>();
}
