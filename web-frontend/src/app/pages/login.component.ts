import { Component } from '@angular/core';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <section>
      <h1>Connexion</h1>
      <p class="tf-placeholder">
        Squelette de connexion — l'intégration Keycloak OIDC (Authorization Code + PKCE)
        sera implémentée à l'étape 4.
      </p>
    </section>
  `,
  styles: [`
    h1 { color: #1a2238; }
    .tf-placeholder { color: #64748b; font-style: italic; }
  `]
})
export class LoginComponent {}
