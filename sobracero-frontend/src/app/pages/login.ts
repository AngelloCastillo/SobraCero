import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensajeError } from '../core/api.service';
import { AutenticacionService } from '../core/autenticacion.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card narrow">
      <h1>Ingresar</h1>
      <p class="sub">Accede a tu cuenta de SobraCero.</p>
      <form class="stack" (ngSubmit)="enviar()">
        <div><label>Correo</label><input type="email" name="correo" [(ngModel)]="correo" autocomplete="username" /></div>
        <div><label>Contraseña</label><input type="password" name="clave" [(ngModel)]="clave" autocomplete="current-password" /></div>
        <div class="err">{{ error() }}</div>
        <button class="btn" [disabled]="cargando()">{{ cargando() ? 'Ingresando…' : 'Ingresar' }}</button>
      </form>
      <p class="hint">¿No tienes cuenta? <a routerLink="/registro" class="link">Regístrate</a></p>
      <details class="mt">
        <summary class="link">Cuentas de demostración</summary>
        <div class="hint" style="line-height:1.8">
          Contraseña de todas: <b>Demo1234</b><br />
          <a class="link" (click)="rellenar('consumidor@demo.pe')">consumidor@demo.pe</a> · consumidor<br />
          <a class="link" (click)="rellenar('comercio1@demo.pe')">comercio1@demo.pe</a> · comercio (plan premium)<br />
          <a class="link" (click)="rellenar('ong1@demo.pe')">ong1@demo.pe</a> · organización social
        </div>
      </details>
    </div>
  `,
})
export class Login {
  private auth = inject(AutenticacionService);
  private router = inject(Router);

  correo = '';
  clave = '';
  error = signal('');
  cargando = signal(false);

  rellenar(correo: string) { this.correo = correo; this.clave = 'Demo1234'; }

  enviar() {
    this.error.set('');
    if (!this.correo || !this.clave) return this.error.set('Ingresa tu correo y contraseña');
    this.cargando.set(true);
    this.auth.login(this.correo, this.clave).subscribe({
      next: (s) => this.router.navigateByUrl(this.auth.inicioPara(s.usuario.rol)),
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}
