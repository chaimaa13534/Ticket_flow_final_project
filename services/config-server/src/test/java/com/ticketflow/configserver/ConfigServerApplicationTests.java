package com.ticketflow.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de non-régression : le contexte Spring de config-server démarre sans erreur
 * avec le profil "native". Vérifie que les configurations YAML sont trouvées.
 */
@SpringBootTest
@ActiveProfiles("native")
@EnableConfigServer
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
        // Si le bean ConfigServerApplication est créé sans exception, le serveur démarre.
    }
}
