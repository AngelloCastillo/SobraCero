import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import * as M from './modelos';

/** Mensaje legible a partir de un error HTTP del backend (campo "detail"). */
export function mensajeError(e: unknown): string {
  const err = e as HttpErrorResponse;
  if (err?.status === 0) return 'No se pudo conectar con el servidor. Intenta de nuevo en unos segundos.';
  return err?.error?.detail ?? 'Ocurrió un error inesperado.';
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  // Autenticación
  login(correo: string, clave: string) { return this.http.post<M.RespuestaAutenticacion>(`${this.base}/api/autenticacion/login`, { correo, clave }); }
  registrar(cuerpo: M.RegistroRequest) { return this.http.post<M.RespuestaAutenticacion>(`${this.base}/api/autenticacion/registro`, cuerpo); }

  // Ofertas
  ofertasCercanas(lat: number, lng: number, radioKm: number) {
    return this.http.get<M.OfertaVista[]>(`${this.base}/api/ofertas/cercanas`, { params: { lat, lng, radioKm } });
  }
  crearOferta(cuerpo: M.CrearOferta) { return this.http.post(`${this.base}/api/ofertas`, cuerpo); }
  misOfertas() { return this.http.get<M.Oferta[]>(`${this.base}/api/ofertas/mias`); }

  // Reservas
  reservar(ofertaId: number, cantidad: number, metodoPago: string) {
    return this.http.post<M.ReservaVista>(`${this.base}/api/reservas`, { ofertaId, cantidad, metodoPago });
  }
  misReservas() { return this.http.get<M.ReservaVista[]>(`${this.base}/api/reservas/mias`); }
  canjearReserva(codigo: string) { return this.http.post<M.ReservaVista>(`${this.base}/api/reservas/canjear`, { codigo }); }

  // Donaciones
  misDonaciones() { return this.http.get<M.DonacionVista[]>(`${this.base}/api/donaciones/mias`); }
  donacionesDeComercio() { return this.http.get<M.DonacionVista[]>(`${this.base}/api/donaciones/comercio`); }
  aceptarDonacion(id: number) { return this.http.post<M.DonacionVista>(`${this.base}/api/donaciones/${id}/aceptar`, {}); }
  rechazarDonacion(id: number) { return this.http.post<void>(`${this.base}/api/donaciones/${id}/rechazar`, {}); }
  completarDonacion(codigo: string) { return this.http.post<M.DonacionVista>(`${this.base}/api/donaciones/completar`, { codigo }); }
  registroDonaciones() { return this.http.get<M.FilaRegistro[]>(`${this.base}/api/donaciones/registro`); }

  // Comercios y organizaciones
  miComercio() { return this.http.get<M.Comercio>(`${this.base}/api/comercios/yo`); }
  comercios() { return this.http.get<M.Comercio[]>(`${this.base}/api/comercios`); }
  validarComercio(id: number) { return this.http.patch<M.Comercio>(`${this.base}/api/comercios/${id}/validar`, {}); }
  rechazarComercio(id: number, motivo: string) { return this.http.patch<M.Comercio>(`${this.base}/api/comercios/${id}/rechazar`, { motivo }); }
  cambiarPlan(id: number, plan: string) { return this.http.patch<M.Comercio>(`${this.base}/api/comercios/${id}/plan`, { plan }); }
  miOrganizacion() { return this.http.get<M.Organizacion>(`${this.base}/api/organizaciones/yo`); }
  organizaciones() { return this.http.get<M.Organizacion[]>(`${this.base}/api/organizaciones`); }
  validarOrganizacion(id: number, deducibleSunat: boolean) { return this.http.patch<M.Organizacion>(`${this.base}/api/organizaciones/${id}/validar`, { deducibleSunat }); }
  rechazarOrganizacion(id: number, motivo: string) { return this.http.patch<M.Organizacion>(`${this.base}/api/organizaciones/${id}/rechazar`, { motivo }); }
  fijarCapacidad(capacidadDiariaKg: number) { return this.http.patch<M.Organizacion>(`${this.base}/api/organizaciones/yo/capacidad`, { capacidadDiariaKg }); }

  // Impacto, predicción y notificaciones
  impactoComercio() { return this.http.get<M.ImpactoComercio>(`${this.base}/api/impacto/comercio`); }
  impactoOrganizacion() { return this.http.get<M.ImpactoOrganizacion>(`${this.base}/api/impacto/organizacion`); }
  impactoPlataforma() { return this.http.get<M.ImpactoPlataforma>(`${this.base}/api/impacto/plataforma`); }
  prediccion() { return this.http.get<M.Prediccion>(`${this.base}/api/prediccion/comercio`); }
  descargarReporte(): Observable<Blob> {
    return this.http.get(`${this.base}/api/impacto/comercio/reporte.csv`, { responseType: 'blob' });
  }
  notificaciones() { return this.http.get<M.Notificaciones>(`${this.base}/api/notificaciones`); }
  leerTodasNotificaciones() { return this.http.post<void>(`${this.base}/api/notificaciones/leer-todas`, {}); }
}
