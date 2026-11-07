package com.ticketflow.customerservice.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fournit un bean Clock injectable dans les services et repositories.
 *
 * Avantage : les tests peuvent injecter un Clock fixe (Clock.fixed) pour
 * rendre les tests déterministes sur les dates.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}