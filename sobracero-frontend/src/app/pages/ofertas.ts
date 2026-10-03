import { DatePipe, DecimalPipe } from '@angular/common';
import { AfterViewInit, Component, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { ApiService, mensajeError } from '../core/api.service';
import { AutenticacionService } from '../core/autenticacion.service';
import { OfertaVista, ReservaVista } from '../core/modelos';

const ICONO = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
});

@Component({
  selector: 'app-ofertas',
  imports: [FormsModule, DatePipe, DecimalPipe, RouterLink],
  template: `
    <div class="hero">
      <h1>Rescata comida cerca de ti</h1>
      <p>Alimentos en buen estado a precio reducido antes de que se pierdan. Lo que no se vende se dona.</p>
    </div>

    <div class="card filters">
      <div>
        <label>Radio</label>
        <select [(ngModel)]="radioKm" (ngModelChange)="buscar()">
          <option [ngValue]="0.5">500 m</option><option [ngValue]="1">1 km</option>
          <option [ngValue]="3">3 km</option><option [ngValue]="10">10 km</option>
        </select>
      </div>
      <div>
        <label>Categoría</label>
        <select [(ngModel)]="categoria" (ngModelChange)="aplicarFiltros()">
          <option value="">Todas</option><option>Panadería</option><option>Comida preparada</option>
          <option>Lácteos</option><option>Frutas y verduras</option>
        </select>
      </div>
      <div>
        <label>Precio máx. (S/)</label>
        <input type="number" [(ngModel)]="precioMaximo" (ngModelChange)="aplicarFiltros()" min="1" />
      </div>
      <div style="flex:none;display:flex;gap:8px">
        <button class="btn sec" (click)="usarMiUbicacion()">📍 Mi ubicación</button>
        <button class="btn" (click)="buscar()">Buscar</button>
      </div>
    </div>

    @if (error()) { <div class="warn">{{ error() }}</div> }

    <div class="split">
      <div class="list">
        @for (o of visibles(); track o.id) {
          <div class="card oferta">
            <div class="emoji">{{ emoji(o.categoria) }}</div>
            <div style="flex:1">
              <div class="row"><h3>{{ o.titulo }}</h3><span class="tag">-{{ descuento(o) }}%</span></div>
              <div class="meta">{{ o.comercioNombre }} ·
                {{ o.distanciaKm < 1 ? (o.distanciaKm * 1000 | number: '1.0-0') + ' m' : (o.distanciaKm | number: '1.1-1') + ' km' }}</div>
              <div class="meta">Vence: <b>{{ o.venceEl }}</b> · Recojo {{ o.recojoDesde | date: 'HH:mm' }}–{{ o.recojoHasta | date: 'HH:mm' }} · Quedan {{ o.cantidad }}</div>
              <div class="row">
                <div class="price"><b>S/ {{ o.precioDescuento | number: '1.2-2' }}</b><s>S/ {{ o.precioOriginal | number: '1.2-2' }}</s></div>
                <button class="btn sm" (click)="reservar(o)">Reservar</button>
              </div>
            </div>
          </div>
        } @empty {
          <div class="card empty">
            {{ cargando() ? 'Buscando ofertas…' : 'No hay ofertas cerca de ti en este momento. Prueba ampliar el radio de búsqueda.' }}
          </div>
        }
      </div>
      <div id="mapa" class="map"></div>
    </div>

    @if (seleccionada(); as o) {
      <div class="modal" (click)="cerrar()">
        <div class="card" (click)="$event.stopPropagation()">
          @if (!hecha()) {
            <h2>Reservar y pagar</h2>
            <p><b>{{ o.titulo }}</b><br /><span class="meta">{{ o.comercioNombre }} · {{ o.comercioDireccion }}</span></p>
            @if (!auth.usuario()) {
              <div class="warn">Inicia sesión como consumidor para reservar.</div>
              <div class="row"><button class="btn sec" (click)="cerrar()">Cancelar</button><a class="btn" routerLink="/login">Ingresar</a></div>
            } @else if (auth.rol() !== 'CONSUMIDOR') {
              <div class="warn">Solo las cuentas de consumidor pueden reservar.</div>
              <button class="btn sec" (click)="cerrar()">Cerrar</button>
            } @else {
              <div class="grid two" style="gap:10px">
                <div><label>Cantidad</label><input type="number" min="1" [max]="o.cantidad" [(ngModel)]="cantidad" /></div>
                <div><label>Total</label><div class="big" style="font-size:24px">S/ {{ o.precioDescuento * cantidad | number: '1.2-2' }}</div></div>
              </div>
              <div style="margin:12px 0">
                <label>Método de pago (modo de prueba)</label>
                <select [(ngModel)]="metodoPago">
                  <option value="YAPE">Yape</option><option value="PLIN">Plin</option><option value="TARJETA">Tarjeta</option>
                  <option value="TARJETA_RECHAZADA">Tarjeta sin fondos (simular rechazo)</option>
                </select>
              </div>
              <div class="err">{{ errorPago() }}</div>
              <div class="row">
                <button class="btn sec" (click)="cerrar()">Cancelar</button>
                <button class="btn" [disabled]="pagando()" (click)="pagar(o)">{{ pagando() ? 'Procesando…' : 'Pagar' }}</button>
              </div>
            }
          } @else {
            <h2>¡Reserva confirmada! 🎉</h2>
            <p class="meta">Muestra este código en {{ hecha()!.comercioNombre }} entre las {{ hecha()!.recojoDesde | date: 'HH:mm' }} y las {{ hecha()!.recojoHasta | date: 'HH:mm' }}.</p>
            <div class="code">{{ hecha()!.codigo }}</div>
            <div class="row">
              <a class="btn sec" routerLink="/mis-reservas" (click)="cerrar()">Ver mis reservas</a>
              <button class="btn" (click)="cerrar()">Listo</button>
            </div>
          }
        </div>
      </div>
    }
  `,
})
export class Ofertas implements AfterViewInit, OnDestroy {
  private api = inject(ApiService);
  auth = inject(AutenticacionService);

  private centro: [number, number] = [-12.077, -77.083]; // San Miguel, Lima
  radioKm = 3;
  categoria = '';
  precioMaximo = 50;

  ofertas = signal<OfertaVista[]>([]);
  visibles = signal<OfertaVista[]>([]);
  cargando = signal(false);
  error = signal('');
  seleccionada = signal<OfertaVista | null>(null);
  hecha = signal<ReservaVista | null>(null);
  metodoPago = 'YAPE';
  errorPago = signal('');
  pagando = signal(false);
  cantidad = 1;

  private mapa?: L.Map;
  private capa = L.layerGroup();

  ngAfterViewInit() {
    this.mapa = L.map('mapa').setView(this.centro, 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap' }).addTo(this.mapa);
    this.capa.addTo(this.mapa);
    L.circleMarker(this.centro, { radius: 8, color: '#1f7a4d', fillOpacity: 0.9 }).addTo(this.mapa).bindPopup('Tú estás aquí');
    this.buscar();
  }

  ngOnDestroy() { this.mapa?.remove(); }

  aplicarFiltros() {
    this.visibles.set(this.ofertas().filter((o) => (!this.categoria || o.categoria === this.categoria) && o.precioDescuento <= this.precioMaximo));
    this.capa.clearLayers();
    for (const o of this.visibles()) {
      L.marker([o.latitud, o.longitud], { icon: ICONO })
        .bindPopup(`<b>${o.titulo}</b><br>${o.comercioNombre}<br>S/ ${o.precioDescuento.toFixed(2)}`).addTo(this.capa);
    }
  }

  buscar() {
    this.cargando.set(true);
    this.error.set('');
    this.api.ofertasCercanas(this.centro[0], this.centro[1], this.radioKm).subscribe({
      next: (res) => { this.ofertas.set(res); this.cargando.set(false); this.aplicarFiltros(); },
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  usarMiUbicacion() {
    navigator.geolocation?.getCurrentPosition(
      (p) => { this.centro = [p.coords.latitude, p.coords.longitude]; this.mapa?.setView(this.centro, 14); this.buscar(); },
      () => this.error.set('No se pudo obtener tu ubicación.'),
    );
  }

  reservar(o: OfertaVista) { this.cantidad = 1; this.errorPago.set(''); this.hecha.set(null); this.seleccionada.set(o); }
  cerrar() { this.seleccionada.set(null); this.hecha.set(null); }

  pagar(o: OfertaVista) {
    this.errorPago.set('');
    if (this.cantidad < 1 || this.cantidad > o.cantidad) return this.errorPago.set(`Elige entre 1 y ${o.cantidad} unidades`);
    this.pagando.set(true);
    this.api.reservar(o.id, this.cantidad, this.metodoPago).subscribe({
      next: (r) => { this.pagando.set(false); this.hecha.set(r); this.buscar(); },
      error: (e) => { this.pagando.set(false); this.errorPago.set(mensajeError(e)); },
    });
  }

  descuento(o: OfertaVista) { return Math.round((1 - o.precioDescuento / o.precioOriginal) * 100); }
  emoji(categoria: string) {
    return ({ 'Panadería': '🥖', 'Comida preparada': '🍱', 'Lácteos': '🥛', 'Frutas y verduras': '🍎' } as Record<string, string>)[categoria] ?? '🍽️';
  }
}
