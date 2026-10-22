# Schéma de la base de données

> Généré depuis `information_schema` via `psql` (PostgreSQL 16).  
> Les migrations sont gérées par **Liquibase** — `ddl-auto: none`.

---

## Diagramme des relations

```
categories ──────────────────────────────┐
  id (PK)                                │
  nom (UNIQUE)                           │ id_categorie (FK)
  description                            │
                                     produits
                                       id (PK)
                                       nom
                                       description
                                       prix
                                       unite_par_packaging
                                       id_categorie → categories.id
                                       date_creation
                                       date_modification
                                           │
                                           │ id_produit (FK)
roles ──────────────┐                     │
  id (PK)           │             lignes_commande
  code (UNIQUE)     │ role_id (FK)   id (PK)
  libelle           │               quantite
                    │               prix_unitaire (snapshot)
               utilisateurs         id_commande → commandes.id
                 id (PK)            id_produit  → produits.id
                 email (UNIQUE)         │
                 mot_de_passe           │ id_utilisateur (FK)
                 nom                    │
                 prenom             commandes ──────────────────┐
                 role_id → roles.id    id (PK)                  │
                 reset_token           montant_total             │ id (FK)
                 reset_token_expiry    id_utilisateur → util.id  │
                 date_creation         statut_id → statuts.id    │
                 date_modification     date_creation             │
                                       date_modification         │
                                                        statuts_commande
                                                          id (PK)
                                                          code (UNIQUE)
                                                          libelle
```

---

## Tables

### `categories`

| Colonne | Type | Nullable | Contraintes |
|---------|------|----------|-------------|
| `id` | `bigint` | NO | PK |
| `nom` | `varchar(100)` | NO | UNIQUE |
| `description` | `varchar(500)` | YES | |

---

### `produits`

| Colonne | Type | Nullable | Contraintes |
|---------|------|----------|-------------|
| `id` | `bigint` | NO | PK |
| `nom` | `varchar(100)` | NO | |
| `description` | `varchar(500)` | YES | |
| `prix` | `numeric(10,2)` | YES | |
| `unite_par_packaging` | `integer` | NO | défaut : 1 |
| `id_categorie` | `bigint` | YES | FK → `categories.id` |
| `date_creation` | `timestamp` | NO | géré par Spring Auditing |
| `date_modification` | `timestamp` | NO | géré par Spring Auditing |

---

### `utilisateurs`

| Colonne | Type | Nullable | Contraintes |
|---------|------|----------|-------------|
| `id` | `bigint` | NO | PK |
| `email` | `varchar(150)` | NO | UNIQUE |
| `mot_de_passe` | `varchar(255)` | NO | hashé BCrypt |
| `nom` | `varchar(100)` | NO | |
| `prenom` | `varchar(100)` | NO | défaut : `''` |
| `role_id` | `bigint` | NO | FK → `roles.id` |
| `reset_token` | `varchar(255)` | YES | token reset password |
| `reset_token_expiry` | `timestamp` | YES | expiration du token |
| `date_creation` | `timestamp` | NO | |
| `date_modification` | `timestamp` | NO | |

---

### `roles`

| Colonne | Type | Nullable | Contraintes |
|---------|------|----------|-------------|
| `id` | `bigint` | NO | PK, auto-increment |
| `code` | `varchar(50)` | NO | UNIQUE |
| `libelle` | `varchar(100)` | NO | |

---

### `commandes`

| Colonne | Type | Nullable | Contraintes |
|---------|------|----------|-------------|
| `id` | `bigint` | NO | PK |
| `montant_total` | `numeric(10,2)` | NO | |
| `id_utilisateur` | `bigint` | NO | FK → `utilisateurs.id` |
| `statut_id` | `bigint` | NO | FK → `statuts_commande.id` |
| `date_creation` | `timestamp` | NO | géré par Spring Auditing |
| `date_modification` | `timestamp` | NO | géré par Spring Auditing |

---

### `lignes_commande`

| Colonne | Type | Nullable | Contraintes |
|---------|------|----------|-------------|
| `id` | `bigint` | NO | PK |
| `quantite` | `integer` | NO | |
| `prix_unitaire` | `numeric(10,2)` | NO | snapshot du prix à la création |
| `id_commande` | `bigint` | NO | FK → `commandes.id` |
| `id_produit` | `bigint` | NO | FK → `produits.id` |

---

### `statuts_commande`

| Colonne | Type | Nullable | Contraintes |
|---------|------|----------|-------------|
| `id` | `bigint` | NO | PK, auto-increment |
| `code` | `varchar(20)` | NO | UNIQUE |
| `libelle` | `varchar(100)` | NO | |

---

## Clés étrangères

| Table | Colonne | Référence |
|-------|---------|-----------|
| `produits` | `id_categorie` | `categories.id` |
| `utilisateurs` | `role_id` | `roles.id` |
| `commandes` | `id_utilisateur` | `utilisateurs.id` |
| `commandes` | `statut_id` | `statuts_commande.id` |
| `lignes_commande` | `id_commande` | `commandes.id` |
| `lignes_commande` | `id_produit` | `produits.id` |

---

## Migrations Liquibase

```
src/main/resources/db/changelog/changes/
├── 001-create-categories.yaml
├── 002-insert-categories.yaml
├── 003-create-produits.yaml
├── 004-create-utilisateurs.yaml
├── 005-add-prenom-utilisateurs.yaml
├── 006-insert-premier-utilisateur.yaml
├── 007-add-id-utilisateur-roles.yaml
├── 008-create-commandes.yaml
├── 009-create-lignes-commande.yaml
├── 010-create-statuts-commande.yaml
├── 011-insert-statuts-commande.yaml
├── 012-migrate-commandes-statut.yaml
├── 013-add-id-to-statuts-commande.yaml
├── 014-create-roles.yaml
├── 015-migrate-utilisateur-role.yaml
└── 016-add-reset-token-utilisateurs.yaml
```

*Dernière mise à jour : 2026-10-22*
