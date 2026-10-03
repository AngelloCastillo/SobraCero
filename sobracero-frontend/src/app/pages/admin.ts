import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, mensajeError } from '../core/api.service';
import { Comercio, ImpactoPlataforma, Organizacion } from '../core/modelos';

@Component({
  selector: 'app-panel-admin',
  imports: [FormsModule, DecimalPipe],
  template: `
    <h1>Panel de administración</h1>
    <p class="sub">Impacto de la plataforma y validación de cuentas.</p>

    @if (impacto(); as i) {
      <div class="grid four">
        <div class="card stat"><b>{{ i.rescatadoKg | number: '1.0-1' }} kg</b><span>Kilos rescatados</span></div>
        <div class="card stat"><b>{{ i.co2EvitadoKg | number: '1.0-1' }} kg</b><span>CO₂ evitado</span></div>
        <div class="card stat"><b>{{ i.comerciosActivos }}</b><span>Comercios activos</span></div>
        <div class="card stat"><b>{{ i.organizacionesActivas }}</b><span>Organizaciones activas</span></div>
      </div>
      <div class="card mt">
        <div class="row"><b>Meta: 500 toneladas en 3 años</b><span class="hint">{{ i.avanceMetaPct | number: '1.0-2' }}%</span></div>
        <div class="progress mt"><div [style.width.%]="Math.max(i.avanceMetaPct, 1)"></div></div>
      </div>
    }

    @if (error()) { <div class="err mt">{{ error() }}</div> }

    <div class="card mt">
      <h2>Comercios</h2>
      <table>
        <thead><tr><th>Nombre</th><th>RUC</th><th>Distrito</th><th>Estado</th><th>Plan</th><th></th></tr></thead>
        <tbody>
          @for (c of comercios(); track c.id) {
            <tr>
              <td><b>{{ c.nombre }}</b></td><td>{{ c.ruc }}</td><td>{{ c.distrito }}</td>
              <td><span class="tag" [class.g]="c.estado === 'VALIDADA'" [class.r]="c.estado === 'RECHAZADA'">{{ etiqueta(c.estado) }}</span></td>
              <td><select [ngModel]="c.plan" (ngModelChange)="cambiarPlan(c, $event)"><option>GRATIS</option><option>PREMIUM</option></select></td>
              <td class="right-actions">
                <button class="btn sm" [disabled]="c.estado === 'VALIDADA'" (click)="validarComercio(c)">Validar</button>
                <button class="btn red sm" [disabled]="c.estado === 'RECHAZADA'" (click)="rechazarComercio(c)">Rechazar</button>
              </td>
            </tr>
          } @empty { <tr><td colspan="6" class="empty">No hay comercios registrados.</td></tr> }
        </tbody>
      </table>
    </div>

    <div class="card mt">
      <h2>Organizaciones sociales</h2>
      <table>
        <thead><tr><th>Nombre</th><th>Dirección</th><th>Estado</th><th>SUNAT</th><th></th></tr></thead>
        <tbody>
          @for (o of organizaciones(); track o.id) {
            <tr>
              <td><b>{{ o.nombre }}</b></td><td>{{ o.direccion }}</td>
              <td><span class="tag" [class.g]="o.estado === 'VALIDADA'" [class.r]="o.estado === 'RECHAZADA'">{{ etiqueta(o.estado) }}</span></td>
              <td><label style="text-transform:none;display:flex;gap:6px;align-items:center;font-size:13px"><input type="checkbox" style="width:auto" [(ngModel)]="sunat[o.id]" [ngModelOptions]="{standalone: true}" /> Entidad perceptora</label></td>
              <td class="right-actions">
                <button class="btn sm" (click)="validarOrganizacion(o)">{{ o.estado === 'VALIDADA' ? 'Actualizar' : 'Validar' }}</button>
                <button class="btn red sm" [disabled]="o.estado === 'RECHAZADA'" (click)="rechazarOrganizacion(o)">Rechazar</button>
              </td>
            </tr>
          } @empty { <tr><td colspan="5" class="empty">No hay organizaciones registradas.</td></tr> }
        </tbody>
      </table>
    </div>
  `,
})
export class PanelAdmin implements OnInit {
  private api = inject(ApiService);
  protected Math = Math;

  comercios = signal<Comercio[]>([]);
  organizaciones = signal<Organizacion[]>([]);
  impacto = signal<ImpactoPlataforma | null>(null);
  error = signal('');
  sunat: Record<number, boolean> = {};

  ngOnInit() { this.cargar(); }

  cargar() {
    this.api.comercios().subscribe({ next: (r) => this.comercios.set(r), error: (e) => this.error.set(mensajeError(e)) });
    this.api.organizaciones().subscribe({
      next: (r) => { this.organizaciones.set(r); r.forEach((o) => (this.sunat[o.id] = o.deducibleSunat)); },
      error: (e) => this.error.set(mensajeError(e)),
    });
    this.api.impactoPlataforma().subscribe((i) => this.impacto.set(i));
  }

  private listo = { next: () => this.cargar(), error: (e: unknown) => this.error.set(mensajeError(e)) };

  validarComercio(c: Comercio) { this.error.set(''); this.api.validarComercio(c.id).subscribe(this.listo); }
  rechazarComercio(c: Comercio) {
    const motivo = prompt(`Motivo del rechazo de "${c.nombre}":`, 'Documentación incompleta (falta licencia de funcionamiento)');
    if (motivo) this.api.rechazarComercio(c.id, motivo).subscribe(this.listo);
  }
  cambiarPlan(c: Comercio, plan: string) { this.error.set(''); this.api.cambiarPlan(c.id, plan).subscribe(this.listo); }
  validarOrganizacion(o: Organizacion) { this.error.set(''); this.api.validarOrganizacion(o.id, !!this.sunat[o.id]).subscribe(this.listo); }
  rechazarOrganizacion(o: Organizacion) {
    const motivo = prompt(`Motivo del rechazo de "${o.nombre}":`, 'Documentación incompleta');
    if (motivo) this.api.rechazarOrganizacion(o.id, motivo).subscribe(this.listo);
  }
  etiqueta(s: string) { return ({ PENDIENTE: 'Pendiente', VALIDADA: 'Validada', RECHAZADA: 'Rechazada' } as Record<string, string>)[s] ?? s; }
}
