import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="tf-header">
      <div class="tf-brand">
        <a routerLink="/">TicketFlow</a>
      </div>
      <nav class="tf-nav">
        <a routerLink="/events">Catalogue</a>
        <a routerLink="/login">Connexion</a>
      </nav>
    </header>
    <main class="tf-main">
      <router-outlet></router-outlet>
    </main>
    <footer class="tf-footer">
      <span>TicketFlow — plateforme de billetterie microservices</span>
    </footer>
  `,
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; font-family: 'Inter', system-ui, sans-serif; }
    .tf-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 2rem; background: #1a2238; color: #fff; }
    .tf-brand a { color: #fff; text-decoration: none; font-size: 1.25rem; font-weight: 700; }
    .tf-nav a { color: #cbd5e1; margin-left: 1.5rem; text-decoration: none; }
    .tf-nav a:hover { color: #fff; }
    .tf-main { flex: 1; padding: 2rem; max-width: 1200px; margin: 0 auto; width: 100%; box-sizing: border-box; }
    .tf-footer { padding: 1rem 2rem; text-align: center; color: #64748b; font-size: 0.875rem; border-top: 1px solid #e2e8f0; }
  `]
})
export class AppComponent {
  title = 'TicketFlow';
}
