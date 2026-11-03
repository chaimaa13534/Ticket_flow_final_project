package com.ticketflow.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Serveur de configuration centralisée.
 *
 * Backends supportés (sélection via le profil Spring actif) :
 *  - "native" : lit les fichiers YAML dans config-repo/ (monté en volume Docker) — utilisé pour la démo.
 *  - "git" (défaut) : clone un dépôt Git — utilisé pour la production.
 *
 * Aucune donnée secrète n'est servie ici : les clés API et mots de passe sont injectés
 * au runtime par les variables d'environnement des services, jamais via ce serveur.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
