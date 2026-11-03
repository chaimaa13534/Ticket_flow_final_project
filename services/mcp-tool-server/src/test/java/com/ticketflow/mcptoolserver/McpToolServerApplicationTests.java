package com.ticketflow.mcptoolserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
class McpToolServerApplicationTests {

    @Test
    void contextLoads() {
        // Contexte Spring OK (stateless, pas de BDD).
    }
}
