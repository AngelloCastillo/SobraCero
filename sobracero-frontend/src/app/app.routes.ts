import { Routes } from '@angular/router';
import { guardaRol } from './core/guardas';

export const rutas: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./pages/ofertas').then((m) => m.Ofertas) },
  { path: 'login', loadComponent: () => import('./pages/login').then((m) => m.Login) },
  { path: 'registro', loadComponent: () => import('./pages/registro').then((m) => m.Registro) },
  { path: 'mis-reservas', canActivate: [guardaRol('CONSUMIDOR')], loadComponent: () => import('./pages/mis-reservas').then((m) => m.MisReservas) },
  { path: 'comercio', canActivate: [guardaRol('COMERCIO')], loadComponent: () => import('./pages/comercio').then((m) => m.PanelComercio) },
  { path: 'organizacion', canActivate: [guardaRol('ORGANIZACION')], loadComponent: () => import('./pages/organizacion').then((m) => m.PanelOrganizacion) },
  { path: 'admin', canActivate: [guardaRol('ADMINISTRADOR')], loadComponent: () => import('./pages/admin').then((m) => m.PanelAdmin) },
  { path: '**', redirectTo: '' },
];
