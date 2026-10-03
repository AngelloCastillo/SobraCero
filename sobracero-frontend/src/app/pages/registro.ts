import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensajeError } from '../core/api.service';
import { AutenticacionService } from '../core/autenticacion.service';
import { RegistroRequest, Rol } from '../core/modelos';

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card narrow">
      <h1>Crear cuenta</h1>
      <p class="sub">Elige cómo quieres participar.</p>
      <div class="tabs">
        <button type="button" [class.on]="rol === 'CONSUMIDOR'" (click)="rol = 'CONSUMIDOR'">🛒 Consumidor</button>
        <button type="button" [class.on]="rol === 'COMERCIO'" (click)="rol = 'COMERCIO'">🏪 Comercio</button>
        <button type="button" [class.on]="rol === 'ORGANIZACION'" (click)="rol = 'ORGANIZACION'">🤝 Organización</button>
      </div>
      <form class="stack" (ngSubmit)="enviar()">
        <div><label>{{ rol === 'CONSUMIDOR' ? 'Tu nombre' : 'Nombre ' + (rol === 'COMERCIO' ? 'del comercio' : 'de la organización') }}</label>
          <input name="nombre" [(ngModel)]="nombre" /></div>
        <div><label>Correo</label><input type="email" name="correo" [(ngModel)]="correo" autocomplete="username" /></div>
        <div><label>Contraseña (mín. 8 caracteres)</label><input type="password" name="clave" [(ngModel)]="clave" autocomplete="new-password" /></div>

        @if (rol !== 'CONSUMIDOR') {
          @if (rol === 'COMERCIO') {
            <div class="grid two" style="gap:10px">
              <div><label>RUC (11 dígitos)</label><input name="ruc" [(ngModel)]="ruc" maxlength="11" /></div>
              <div><label>Distrito</label><input name="distrito" [(ngModel)]="distrito" /></div>
            </div>
          } @else {
            <div><label>Capacidad diaria de recojo (kg)</label><input type="number" name="capacidad" [(ngModel)]="capacidad" min="0" /></div>
          }
          <div><label>Dirección</label><input name="direccion" [(ngModel)]="direccion" /></div>
          <div class="grid two" style="gap:10px">
            <div><label>Latitud</label><input type="number" step="0.0001" name="latitud" [(ngModel)]="latitud" /></div>
            <div><label>Longitud</label><input type="number" step="0.0001" name="longitud" [(ngModel)]="longitud" /></div>
          </div>
          <button type="button" class="btn sec sm" (click)="ubicar()">📍 Usar mi ubicación actual</button>
          <div class="hint">Tu cuenta quedará pendiente hasta que un administrador la valide.</div>
        }
        <div class="err">{{ error() }}</div>
        <button class="btn" [disabled]="cargando()">{{ cargando() ? 'Creando…' : 'Crear cuenta' }}</button>
      </form>
      <p class="hint">¿Ya tienes cuenta? <a routerLink="/login" class="link">Ingresa</a></p>
    </div>
  `,
})
export class Registro {
  private auth = inject(AutenticacionService);
  private router = inject(Router);

  rol: Rol = 'CONSUMIDOR';
  nombre = ''; correo = ''; clave = ''; ruc = ''; distrito = ''; direccion = '';
  latitud: number | null = -12.077; longitud: number | null = -77.083; capacidad: number | null = 40;
  error = signal('');
  cargando = signal(false);

  ubicar() {
    navigator.geolocation?.getCurrentPosition(
      (p) => { this.latitud = +p.coords.latitude.toFixed(5); this.longitud = +p.coords.longitude.toFixed(5); },
      () => this.error.set('No se pudo obtener tu ubicación; escribe las coordenadas.'),
    );
  }

  enviar() {
    this.error.set('');
    if (!this.nombre.trim()) return this.error.set('El nombre es obligatorio');
    if (!this.correo.trim()) return this.error.set('El correo es obligatorio');
    if (this.clave.length < 8) return this.error.set('La contraseña debe tener al menos 8 caracteres');
    const cuerpo: RegistroRequest = { correo: this.correo, clave: this.clave, rol: this.rol, nombre: this.nombre };
    if (this.rol !== 'CONSUMIDOR') {
      cuerpo.direccion = this.direccion; cuerpo.latitud = this.latitud ?? undefined; cuerpo.longitud = this.longitud ?? undefined;
      if (this.rol === 'COMERCIO') { cuerpo.ruc = this.ruc; cuerpo.distrito = this.distrito; }
      else cuerpo.capacidadDiariaKg = this.capacidad ?? undefined;
    }
    this.cargando.set(true);
    this.auth.registrar(cuerpo).subscribe({
      next: (s) => this.router.navigateByUrl(this.auth.inicioPara(s.usuario.rol)),
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}
