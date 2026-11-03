import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home.component';
import { EventsComponent } from './pages/events.component';
import { LoginComponent } from './pages/login.component';

// Routes publiques par défaut. Les routes privées (mes réservations, back-office)
// seront ajoutées aux étapes suivantes avec un AuthGuard.
export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'TicketFlow — Accueil' },
  { path: 'events', component: EventsComponent, title: 'Catalogue — TicketFlow' },
  { path: 'login', component: LoginComponent, title: 'Connexion — TicketFlow' },
  { path: '**', redirectTo: '' }
];
