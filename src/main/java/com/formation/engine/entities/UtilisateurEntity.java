package com.formation.engine.entities;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "utilisateurs")
@EntityListeners(AuditingEntityListener.class)
public class UtilisateurEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime date_creation;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime date_modification;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column
    private String motDePasse;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(nullable = false)
    private boolean actif = false;

    @Column
    private String resetToken;

    @Column
    private LocalDateTime resetTokenExpiry;

    @Column(nullable = false)
    private int tentativesEchouees = 0;

    @Column
    private LocalDateTime blocageJusqua;

    public boolean estBloque() {
        return blocageJusqua != null && LocalDateTime.now(ZoneOffset.UTC).isBefore(blocageJusqua);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
