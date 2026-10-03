import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, mensajeError } from '../core/api.service';
import { DonacionVista, FilaRegistro, ImpactoOrganizacion, Organizacion } from '../core/modelos';

@Component({
  selector: 'app-panel-organizacion',
  imports: [FormsModule, DatePipe, DecimalPipe],
  template: `
    @if (organizacion(); as o) {
      <h1>{{ o.nombre }}
        <span class="tag" [class.g]="o.estado === 'VALIDADA'" [class.r]="o.estado === 'RECHAZADA'">{{ etiquetaEstado(o.estado) }}</span>
        @if (o.deducibleSunat) { <span class="tag g">Apta para donaciones deducibles</span> }
      </h1>
      <p class="sub">{{ o.direccion }}</p>
      @if (o.estado === 'PENDIENTE') { <div class="warn">Tu cuenta está pendiente de validación. Recibirás avisos de donaciones cuando un administrador la apruebe.</div> }
      @if (o.estado === 'RECHAZADA') { <div class="warn">Tu cuenta fue rechazada. Motivo: {{ o.motivoRechazo }}</div> }

      @if (impacto(); as i) {
        <div class="grid four">
          <div class="card stat"><b>{{ i.recibidoKg | number: '1.0-1' }} kg</b><span>Alimentos recibidos</span></div>
          <div class="card stat"><b>{{ i.donacionesCompletadas }}</b><span>Donaciones completadas</span></div>
          <div class="card stat"><b>{{ i.co2EvitadoKg | number: '1.0-1' }} kg</b><span>CO₂ evitado</span></div>
          <div class="card stat">
            <label>Capacidad diaria (kg)</label>
            <div class="row" style="gap:6px"><input type="number" min="0" [(ngModel)]="capacidad" /><button class="btn sm" (click)="guardarCapacidad()">Guardar</button></div>
            <div class="hint">{{ mensajeCapacidad() }}</div>
          </div>
        </div>
      }

      <div class="tabs mt">
        <button [class.on]="pestana() === 'activas'" (click)="pestana.set('activas')">📥 Donaciones ({{ activas().length }})</button>
        <button [class.on]="pestana() === 'completar'" (click)="pestana.set('completar')">✅ Confirmar recojo</button>
        <button [class.on]="pestana() === 'registro'" (click)="pestana.set('registro'); cargarRegistro()">📒 Registro (Ley 30498)</button>
      </div>

      @switch (pestana()) {
        @case ('activas') {
          @if (error()) { <div class="err">{{ error() }}</div> }
          <div class="grid two">
            @for (d of activas(); track d.id) {
              <div class="card">
                <div class="row"><h3>{{ d.cantidad }} × {{ d.ofertaTitulo }}</h3><span class="tag">{{ d.pesoKg | number: '1.0-2' }} kg</span></div>
                <div class="meta">{{ d.comercioNombre }} · {{ d.comercioDireccion }} · {{ d.distanciaKm | number: '1.1-1' }} km</div>
                <div class="meta">Recojo hasta las {{ d.recojoHasta | date: 'HH:mm' }} · Código <b>{{ d.codigo }}</b></div>
                @if (d.estado === 'OFRECIDA') {
                  <div class="row mt">
                    <button class="btn sec sm" (click)="rechazar(d)">Rechazar</button>
                    <button class="btn sm" (click)="aceptar(d)">Aceptar</button>
                  </div>
                } @else {
                  <div class="ok">Aceptada · confirma el recojo con el código {{ d.codigo }}</div>
                }
              </div>
            } @empty { <div class="card empty" style="grid-column:1/-1">No hay donaciones disponibles por ahora.</div> }
          </div>
        }
        @case ('completar') {
          <div class="card" style="max-width:480px">
            <h2>Confirmar recojo con código</h2>
            <div class="row" style="gap:10px"><input [(ngModel)]="codigo" placeholder="DON-XXXXX" style="flex:1" /><button class="btn" (click)="completar()">Confirmar</button></div>
            <div class="err">{{ errorCompletar() }}</div>
            @if (okCompletar()) { <div class="ok">{{ okCompletar() }}</div> }
          </div>
        }
        @case ('registro') {
          <div class="card">
            <table>
              <thead><tr><th>Fecha</th><th>Donante (RUC)</th><th>Alimento</th><th>Cantidad</th><th>Peso</th><th>Beneficiario</th></tr></thead>
              <tbody>
                @for (r of registro(); track r.codigo) {
                  <tr><td>{{ r.fecha | date: 'dd/MM/yyyy HH:mm' }}</td><td>{{ r.donante }}<div class="hint">{{ r.donanteRuc }}</div></td>
                    <td>{{ r.tipoAlimento }}</td><td>{{ r.cantidad }}</td><td>{{ r.pesoKg | number: '1.0-2' }} kg</td><td>{{ r.beneficiario }}</td></tr>
                } @empty { <tr><td colspan="6" class="empty">Aún no hay donaciones recibidas.</td></tr> }
              </tbody>
            </table>
          </div>
        }
      }
    } @else {
      <div class="card empty">{{ errorCarga() || 'Cargando…' }}</div>
    }
  `,
})
export class PanelOrganizacion implements OnInit {
  private api = inject(ApiService);

  organizacion = signal<Organizacion | null>(null);
  impacto = signal<ImpactoOrganizacion | null>(null);
  donaciones = signal<DonacionVista[]>([]);
  registro = signal<FilaRegistro[]>([]);
  pestana = signal<'activas' | 'completar' | 'registro'>('activas');
  errorCarga = signal(''); error = signal(''); errorCompletar = signal(''); okCompletar = signal(''); mensajeCapacidad = signal('');
  capacidad = 0;
  codigo = '';

  activas() { return this.donaciones().filter((d) => d.estado === 'OFRECIDA' || d.estado === 'ACEPTADA'); }

  ngOnInit() {
    this.api.miOrganizacion().subscribe({
      next: (o) => { this.organizacion.set(o); this.capacidad = o.capacidadDiariaKg ?? 0; },
      error: (e) => this.errorCarga.set(mensajeError(e)),
    });
    this.recargar();
  }

  recargar() {
    this.api.misDonaciones().subscribe((d) => this.donaciones.set(d));
    this.api.impactoOrganizacion().subscribe((i) => this.impacto.set(i));
  }
  cargarRegistro() { this.api.registroDonaciones().subscribe((r) => this.registro.set(r)); }

  aceptar(d: DonacionVista) { this.error.set(''); this.api.aceptarDonacion(d.id).subscribe({ next: () => this.recargar(), error: (e) => this.error.set(mensajeError(e)) }); }
  rechazar(d: DonacionVista) { this.error.set(''); this.api.rechazarDonacion(d.id).subscribe({ next: () => this.recargar(), error: (e) => this.error.set(mensajeError(e)) }); }

  completar() {
    this.errorCompletar.set(''); this.okCompletar.set('');
    this.api.completarDonacion(this.codigo).subscribe({
      next: (d) => { this.okCompletar.set(`✅ Recojo confirmado y registrado: ${d.cantidad} × ${d.ofertaTitulo}.`); this.codigo = ''; this.recargar(); },
      error: (e) => this.errorCompletar.set(mensajeError(e)),
    });
  }

  guardarCapacidad() {
    this.mensajeCapacidad.set('');
    this.api.fijarCapacidad(this.capacidad).subscribe({ next: () => this.mensajeCapacidad.set('Guardado'), error: (e) => this.mensajeCapacidad.set(mensajeError(e)) });
  }

  etiquetaEstado(s: string) { return ({ PENDIENTE: 'Pendiente', VALIDADA: 'Validada', RECHAZADA: 'Rechazada' } as Record<string, string>)[s] ?? s; }
}
