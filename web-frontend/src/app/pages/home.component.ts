import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <section class="tf-hero">
      <h1>Bienvenue sur TicketFlow</h1>
      <p>Plateforme de billetterie microservices — Spring Boot, Spring Cloud, Kafka, Keycloak.</p>
      <p class="tf-hero__hint">
        Cette page est un squelette. Le contenu métier arrive à l'étape 3.
      </p>
    </section>
  `,
  styles: [`
    .tf-hero { text-align: center; padding: 3rem 1rem; }
    .tf-hero h1 { font-size: 2.5rem; color: #1a2238; margin-bottom: 1rem; }
    .tf-hero p { color: #475569; margin-bottom: 0.5rem; }
    .tf-hero__hint { font-style: italic; color: #94a3b8; font-size: 0.875rem; }
  `]
})
export class HomeComponent {}
