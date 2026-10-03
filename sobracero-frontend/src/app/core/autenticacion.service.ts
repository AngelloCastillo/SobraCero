import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { ApiService } from './api.service';
import { RegistroRequest, RespuestaAutenticacion, Rol, UsuarioVista } from './modelos';

const CLAVE_ALMACEN = 'sobracero.sesion';

@Injectable({ providedIn: 'root' })
export class AutenticacionService {
  private api = inject(ApiService);
  private router = inject(Router);

  private sesion = signal<RespuestaAutenticacion | null>(this.cargar());

  readonly usuario = computed<UsuarioVista | null>(() => this.sesion()?.usuario ?? null);
  readonly token = computed(() => this.sesion()?.token ?? null);
  readonly rol = computed<Rol | null>(() => this.usuario()?.rol ?? null);

  private cargar(): RespuestaAutenticacion | null {
    try {
      const crudo = localStorage.getItem(CLAVE_ALMACEN);
      if (!crudo) return null;
      const s = JSON.parse(crudo) as RespuestaAutenticacion;
      return new Date(s.expiraEn).getTime() > Date.now() ? s : null;
    } catch {
      return null;
    }
  }

  private guardar(s: RespuestaAutenticacion | null) {
    this.sesion.set(s);
    try {
      if (s) localStorage.setItem(CLAVE_ALMACEN, JSON.stringify(s));
      else localStorage.removeItem(CLAVE_ALMACEN);
    } catch { /* almacenamiento no disponible */ }
  }

  login(correo: string, clave: string) {
    return this.api.login(correo, clave).pipe(tap((s) => this.guardar(s)));
  }

  registrar(cuerpo: RegistroRequest) {
    return this.api.registrar(cuerpo).pipe(tap((s) => this.guardar(s)));
  }

  cerrarSesion() {
    this.guardar(null);
    this.router.navigateByUrl('/login');
  }

  /** Ruta de inicio según el rol. */
  inicioPara(rol: Rol | null): string {
    switch (rol) {
      case 'COMERCIO': return '/comercio';
      case 'ORGANIZACION': return '/organizacion';
      case 'ADMINISTRADOR': return '/admin';
      default: return '/';
    }
  }
}
