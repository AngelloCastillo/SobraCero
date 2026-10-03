import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, mensajeError } from '../core/api.service';
import { Comercio, DonacionVista, ImpactoComercio, Oferta, Prediccion } from '../core/modelos';

@Component({
  selector: 'app-panel-comercio',
  imports: [FormsModule, DatePipe, DecimalPipe],
  template: `
    @if (comercio(); as c) {
      <div class="row" style="align-items:flex-start;flex-wrap:wrap">
        <div>
          <h1>{{ c.nombre }}
            <span class="tag" [class.g]="c.estado === 'VALIDADA'" [class.r]="c.estado === 'RECHAZADA'">{{ etiquetaEstado(c.estado) }}</span>
            @if (c.plan === 'PREMIUM') { <span class="tag g">Premium</span> }
          </h1>
          <p class="sub">{{ c.direccion }} · RUC {{ c.ruc }}</p>
        </div>
      </div>

      @if (c.estado === 'PENDIENTE') { <div class="warn">Tu cuenta está pendiente de validación. Podrás publicar ofertas cuando un administrador la apruebe.</div> }
      @if (c.estado === 'RECHAZADA') { <div class="warn">Tu cuenta fue rechazada. Motivo: {{ c.motivoRechazo }}</div> }

      @if (impacto(); as i) {
        <div class="grid four">
          <div class="card stat"><b>{{ i.vendidoKg | number: '1.0-1' }} kg</b><span>Vendidos y recogidos</span></div>
          <div class="card stat"><b>{{ i.donadoKg | number: '1.0-1' }} kg</b><span>Donados</span></div>
          <div class="card stat"><b>S/ {{ i.ingresos | number: '1.2-2' }}</b><span>Ingresos recuperados</span></div>
          <div class="card stat"><b>{{ i.co2EvitadoKg | number: '1.0-1' }} kg</b><span>CO₂ evitado</span></div>
        </div>
        <div class="card mt">
          <div class="row"><h2 style="margin:0">Distintivo de impacto</h2><span class="hint">{{ i.rescatadoKg | number: '1.0-1' }} / {{ i.distintivo.minKg }} kg</span></div>
          <div class="progress mt"><div [style.width.%]="i.distintivo.avancePct"></div></div>
          @if (i.distintivo.otorgado) { <div class="badge-ok mt">🏅 {{ i.distintivo.mensaje }}</div> } @else { <div class="hint">{{ i.distintivo.mensaje }}</div> }
        </div>
      }

      <div class="tabs mt">
        <button [class.on]="pestana() === 'publicar'" (click)="pestana.set('publicar')">➕ Publicar</button>
        <button [class.on]="pestana() === 'ofertas'" (click)="pestana.set('ofertas')">📦 Mis ofertas</button>
        <button [class.on]="pestana() === 'canjear'" (click)="pestana.set('canjear')">🎟️ Canjear código</button>
        <button [class.on]="pestana() === 'donaciones'" (click)="pestana.set('donaciones'); cargarDonaciones()">🤝 Donaciones</button>
        <button [class.on]="pestana() === 'prediccion'" (click)="pestana.set('prediccion'); cargarPrediccion()">📈 Predicción</button>
      </div>

      @switch (pestana()) {
        @case ('publicar') {
          <div class="card" style="max-width:640px">
            <h2>Publicar oferta</h2>
            <div class="stack">
              <div><label>Producto</label><input [(ngModel)]="o.titulo" placeholder="Pan francés (bolsa de 10)" /></div>
              <div class="grid two" style="gap:10px">
                <div><label>Categoría</label>
                  <select [(ngModel)]="o.categoria"><option>Panadería</option><option>Comida preparada</option><option>Lácteos</option><option>Frutas y verduras</option></select></div>
                <div><label>Cantidad (unidades)</label><input type="number" min="1" [(ngModel)]="o.cantidad" /></div>
              </div>
              <div class="grid two" style="gap:10px">
                <div><label>Precio original (S/)</label><input type="number" step="0.1" [(ngModel)]="o.precioOriginal" /></div>
                <div><label>Precio con descuento (S/)</label><input type="number" step="0.1" [(ngModel)]="o.precioDescuento" /></div>
              </div>
              <div class="grid two" style="gap:10px">
                <div><label>Peso por unidad (kg)</label><input type="number" step="0.1" min="0.1" [(ngModel)]="o.pesoUnitarioKg" /></div>
                <div><label>Fecha de vencimiento</label><input type="date" [(ngModel)]="o.venceEl" /></div>
              </div>
              <div class="grid two" style="gap:10px">
                <div><label>Recojo desde</label><input type="time" [(ngModel)]="desde" /></div>
                <div><label>Recojo hasta</label><input type="time" [(ngModel)]="hasta" /></div>
              </div>
              <div class="hint">Si nadie reserva, 15 minutos antes del cierre pasa automáticamente a una organización social.</div>
              <div class="err">{{ errorPublicar() }}</div>
              <button class="btn" [disabled]="c.estado !== 'VALIDADA'" (click)="publicar()">Publicar</button>
              @if (okPublicar()) { <div class="ok">{{ okPublicar() }}</div> }
            </div>
          </div>
        }
        @case ('ofertas') {
          <div class="card">
            <table>
              <thead><tr><th>Producto</th><th>Quedan</th><th>Precio</th><th>Recojo</th><th>Estado</th></tr></thead>
              <tbody>
                @for (x of ofertas(); track x.id) {
                  <tr><td><b>{{ x.titulo }}</b></td><td>{{ x.cantidad }} / {{ x.cantidadInicial }}</td>
                    <td>S/ {{ x.precioDescuento | number: '1.2-2' }}</td>
                    <td>{{ x.recojoDesde | date: 'dd/MM HH:mm' }}–{{ x.recojoHasta | date: 'HH:mm' }}</td>
                    <td><span class="tag" [class.g]="x.estado === 'ACTIVA'" [class.r]="x.estado === 'PERDIDA'" [class.gray]="x.estado === 'VENCIDA'">{{ etiquetaOferta(x.estado) }}</span></td></tr>
                } @empty { <tr><td colspan="5" class="empty">Aún no tienes ofertas.</td></tr> }
              </tbody>
            </table>
          </div>
        }
        @case ('canjear') {
          <div class="card" style="max-width:480px">
            <h2>Canjear código de reserva</h2>
            <div class="row" style="gap:10px"><input [(ngModel)]="codigo" placeholder="SC-XXXXX" style="flex:1" /><button class="btn" (click)="canjear()">Confirmar recojo</button></div>
            <div class="err">{{ errorCanje() }}</div>
            @if (okCanje()) { <div class="ok">{{ okCanje() }}</div> }
          </div>
        }
        @case ('donaciones') {
          <div class="card">
            <table>
              <thead><tr><th>Producto</th><th>Cantidad</th><th>Peso</th><th>Organización</th><th>Estado</th></tr></thead>
              <tbody>
                @for (d of donaciones(); track d.id) {
                  <tr><td><b>{{ d.ofertaTitulo }}</b></td><td>{{ d.cantidad }}</td><td>{{ d.pesoKg | number: '1.0-2' }} kg</td>
                    <td>{{ d.organizacionNombre }}</td>
                    <td><span class="tag" [class.g]="d.estado === 'COMPLETADA'" [class.r]="d.estado === 'RECHAZADA'">{{ etiquetaDonacion(d.estado) }}</span></td></tr>
                } @empty { <tr><td colspan="5" class="empty">Aún no hay donaciones de tus ofertas.</td></tr> }
              </tbody>
            </table>
          </div>
        }
        @case ('prediccion') {
          <div class="card" style="max-width:640px">
            <h2>Predicción de excedentes para mañana</h2>
            @if (prediccion(); as p) {
              <p class="sub">{{ p.mensaje }}</p>
              @for (x of p.predicciones; track x.categoria) {
                <div class="row" style="padding:8px 0;border-bottom:1px solid var(--line)">
                  <b>{{ x.categoria }}</b><span>~ {{ x.unidadesEsperadas }} unidades · descuento sugerido {{ x.descuentoSugeridoPct }}%</span>
                </div>
              }
            } @else if (errorPrediccion()) { <div class="warn">{{ errorPrediccion() }}</div> } @else { <div class="empty">Calculando…</div> }
          </div>
        }
      }

      @if (impacto()) {
        <div class="card mt" style="max-width:640px">
          <h2>Reporte de impacto</h2>
          <p class="sub">Descarga un resumen de kilos rescatados, CO₂ evitado e ingresos (plan Premium).</p>
          <button class="btn sec" (click)="descargarReporte()">⬇️ Descargar reporte (CSV)</button>
          <div class="err">{{ errorReporte() }}</div>
        </div>
      }
    } @else {
      <div class="card empty">{{ errorCarga() || 'Cargando…' }}</div>
    }
  `,
})
export class PanelComercio implements OnInit {
  private api = inject(ApiService);

  comercio = signal<Comercio | null>(null);
  impacto = signal<ImpactoComercio | null>(null);
  ofertas = signal<Oferta[]>([]);
  donaciones = signal<DonacionVista[]>([]);
  prediccion = signal<Prediccion | null>(null);
  pestana = signal<'publicar' | 'ofertas' | 'canjear' | 'donaciones' | 'prediccion'>('publicar');
  errorCarga = signal(''); errorPublicar = signal(''); okPublicar = signal(''); errorCanje = signal('');
  okCanje = signal(''); errorPrediccion = signal(''); errorReporte = signal('');

  // Fechas en hora local (toISOString usaría UTC y en Lima, de noche, daría el día siguiente)
  private static ymd(d: Date) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  private hoy = PanelComercio.ymd(new Date());
  private manana = PanelComercio.ymd(new Date(Date.now() + 86400000));
  o = { titulo: '', categoria: 'Panadería', cantidad: 5, precioOriginal: 10, precioDescuento: 4, pesoUnitarioKg: 0.5, venceEl: this.manana };
  desde = '19:00';
  hasta = '21:00';
  codigo = '';

  ngOnInit() {
    this.api.miComercio().subscribe({ next: (c) => this.comercio.set(c), error: (e) => this.errorCarga.set(mensajeError(e)) });
    this.cargarImpacto();
    this.cargarOfertas();
  }

  cargarImpacto() { this.api.impactoComercio().subscribe((i) => this.impacto.set(i)); }
  cargarOfertas() { this.api.misOfertas().subscribe((x) => this.ofertas.set(x)); }
  cargarDonaciones() { this.api.donacionesDeComercio().subscribe((x) => this.donaciones.set(x)); }
  cargarPrediccion() {
    this.errorPrediccion.set('');
    this.api.prediccion().subscribe({ next: (p) => this.prediccion.set(p), error: (e) => this.errorPrediccion.set(mensajeError(e)) });
  }

  publicar() {
    this.errorPublicar.set(''); this.okPublicar.set('');
    this.api.crearOferta({
      ...this.o,
      recojoDesde: `${this.hoy}T${this.desde}:00`,
      recojoHasta: `${this.hoy}T${this.hasta}:00`,
    }).subscribe({
      next: () => { this.okPublicar.set('✅ Oferta publicada. Ya es visible para consumidores cercanos.'); this.cargarOfertas(); this.o.titulo = ''; },
      error: (e) => this.errorPublicar.set(mensajeError(e)),
    });
  }

  canjear() {
    this.errorCanje.set(''); this.okCanje.set('');
    this.api.canjearReserva(this.codigo).subscribe({
      next: (r) => { this.okCanje.set(`✅ Recojo confirmado: ${r.cantidad} × ${r.ofertaTitulo}.`); this.codigo = ''; this.cargarImpacto(); },
      error: (e) => this.errorCanje.set(mensajeError(e)),
    });
  }

  descargarReporte() {
    this.errorReporte.set('');
    this.api.descargarReporte().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = 'reporte-impacto.csv'; a.click();
        URL.revokeObjectURL(url);
      },
      error: async (e) => {
        try { this.errorReporte.set(JSON.parse(await e.error.text()).detail); } catch { this.errorReporte.set('No se pudo generar el reporte.'); }
      },
    });
  }

  etiquetaEstado(s: string) { return ({ PENDIENTE: 'Pendiente', VALIDADA: 'Validada', RECHAZADA: 'Rechazada' } as Record<string, string>)[s] ?? s; }
  etiquetaOferta(s: string) { return ({ ACTIVA: 'Activa', AGOTADA: 'Agotada', DONADA: 'Donada', VENCIDA: 'Vencida', PERDIDA: 'Pérdida' } as Record<string, string>)[s] ?? s; }
  etiquetaDonacion(s: string) { return ({ OFRECIDA: 'Ofrecida', ACEPTADA: 'Aceptada', COMPLETADA: 'Entregada', RECHAZADA: 'Rechazada' } as Record<string, string>)[s] ?? s; }
}
