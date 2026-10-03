import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService } from '../core/api.service';
import { ReservaVista } from '../core/modelos';

@Component({
  selector: 'app-mis-reservas',
  imports: [DatePipe, DecimalPipe],
  template: `
    <h1>Mis reservas</h1>
    <p class="sub">Muestra el código al comercio dentro de la ventana de recojo.</p>
    <div class="grid two">
      @for (r of reservas(); track r.id) {
        <div class="card">
          <div class="row"><h3>{{ r.cantidad }} × {{ r.ofertaTitulo }}</h3>
            <span class="tag" [class.g]="r.estado === 'RECOGIDA'" [class.r]="r.estado === 'NO_RECOGIDA'">{{ etiqueta(r.estado) }}</span></div>
          <div class="meta">{{ r.comercioNombre }} · {{ r.comercioDireccion }}</div>
          <div class="meta">Recojo {{ r.recojoDesde | date: 'dd/MM HH:mm' }} – {{ r.recojoHasta | date: 'HH:mm' }} · S/ {{ r.monto | number: '1.2-2' }}</div>
          @if (r.estado === 'PAGADA') { <div class="code">{{ r.codigo }}</div> } @else { <div class="meta">Código: {{ r.codigo }}</div> }
        </div>
      } @empty {
        <div class="card empty" style="grid-column:1/-1">{{ cargada() ? 'Aún no tienes reservas. ¡Rescata tu primera comida!' : 'Cargando…' }}</div>
      }
    </div>
  `,
})
export class MisReservas implements OnInit {
  private api = inject(ApiService);
  reservas = signal<ReservaVista[]>([]);
  cargada = signal(false);

  ngOnInit() { this.api.misReservas().subscribe((r) => { this.reservas.set(r); this.cargada.set(true); }); }

  etiqueta(estado: string) { return ({ PAGADA: 'Pendiente de recojo', RECOGIDA: 'Recogida', NO_RECOGIDA: 'No recogida' } as Record<string, string>)[estado] ?? estado; }
}
