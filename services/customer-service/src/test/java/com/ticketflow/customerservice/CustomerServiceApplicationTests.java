package com.ticketflow.customerservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Test de non-régression : le contexte Spring démarre sans erreur avec un profil de test
 * léger (pas de Keycloak, pas de Kafka réel — utilisés via Testcontainers plus tard).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "spring.autoconfigure.exclude[1]=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
class CustomerServiceApplicationTests {

    @Test
    void contextLoads() {
        // Le contexte Spring démarre sans erreur.
    }
}
