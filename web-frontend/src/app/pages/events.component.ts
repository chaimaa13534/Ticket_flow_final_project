import { Component } from '@angular/core';

@Component({
  selector: 'app-events',
  standalone: true,
  template: `
    <section>
      <h1>Catalogue des événements</h1>
      <p class="tf-placeholder">
        Squelette du catalogue — la liste paginée, la recherche et les filtres
        seront branchés sur GET /api/v1/events à l'étape 3a.
      </p>
    </section>
  `,
  styles: [`
    h1 { color: #1a2238; }
    .tf-placeholder { color: #64748b; font-style: italic; }
  `]
})
export class EventsComponent {}
