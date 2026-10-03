import { DatePipe } from '@angular/common';
import { Component, OnDestroy, effect, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from './core/api.service';
import { AutenticacionService } from './core/autenticacion.service';
import { Notificaciones } from './core/modelos';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  template: `
    <header>
      <div class="bar">
        <a routerLink="/" class="logo">Sobra<span>Cero</span></a>
        <nav>
          <a routerLink="/" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }">🛒 Ofertas</a>
          @switch (auth.rol()) {
            @case ('CONSUMIDOR') { <a routerLink="/mis-reservas" routerLinkActive="on">🎟️ Mis reservas</a> }
            @case ('COMERCIO') { <a routerLink="/comercio" routerLinkActive="on">🏪 Mi comercio</a> }
            @case ('ORGANIZACION') { <a routerLink="/organizacion" routerLinkActive="on">🤝 Donaciones</a> }
            @case ('ADMINISTRADOR') { <a routerLink="/admin" routerLinkActive="on">🛡️ Administración</a> }
          }
        </nav>
        <div class="right">
          @if (auth.usuario(); as u) {
            <div class="bell-wrap">
              <button class="bell" (click)="alternarCampana()" aria-label="Notificaciones">
                🔔 @if (notas().sinLeer > 0) { <span class="dot">{{ notas().sinLeer }}</span> }
              </button>
              @if (campanaAbierta()) {
                <div class="dropdown card">
                  <div class="row"><b>Notificaciones</b><a class="link" (click)="leerTodas()">Marcar leídas</a></div>
                  @for (n of notas().items; track n.id) {
                    <div class="note" [class.unread]="!n.leida">
                      <b>{{ n.titulo }}</b>
                      <div>{{ n.mensaje }}</div>
                      <small>{{ n.creadoEn | date: 'dd/MM HH:mm' }}</small>
                    </div>
                  } @empty { <div class="empty">Sin notificaciones</div> }
                </div>
              }
            </div>
            <span class="who">{{ u.nombre || u.correo }}</span>
            <button class="btn sec sm" (click)="auth.cerrarSesion()">Salir</button>
          } @else {
            <a routerLink="/login" class="btn sec sm">Ingresar</a>
            <a routerLink="/registro" class="btn sm">Crear cuenta</a>
          }
        </div>
      </div>
    </header>
    <main><router-outlet /></main>
    <footer>SobraCero © 2026 · Arquitectura de Aplicaciones Web · UPC</footer>
  `,
})
export class App implements OnDestroy {
  auth = inject(AutenticacionService);
  private api = inject(ApiService);

  notas = signal<Notificaciones>({ sinLeer: 0, items: [] });
  campanaAbierta = signal(false);
  private temporizador?: ReturnType<typeof setInterval>;

  constructor() {
    // Consulta las notificaciones al iniciar sesión y luego cada 30 s
    effect(() => {
      clearInterval(this.temporizador);
      if (this.auth.usuario()) {
        this.actualizar();
        this.temporizador = setInterval(() => this.actualizar(), 30000);
      } else {
        this.notas.set({ sinLeer: 0, items: [] });
      }
    });
  }

  ngOnDestroy() { clearInterval(this.temporizador); }

  actualizar() { this.api.notificaciones().subscribe({ next: (n) => this.notas.set(n), error: () => {} }); }
  alternarCampana() { this.campanaAbierta.update((v) => !v); if (this.campanaAbierta()) this.actualizar(); }
  leerTodas() { this.api.leerTodasNotificaciones().subscribe(() => this.actualizar()); }
}
