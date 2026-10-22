package com.formation.engine.exceptions;

public class CommandeNonSupprimableException extends RuntimeException {

    public CommandeNonSupprimableException(Long id, String statut) {
        super("La commande " + id + " ne peut pas être supprimée (statut : " + statut + ")");
    }
}
