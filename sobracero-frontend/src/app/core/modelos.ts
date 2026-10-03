export type Rol = 'COMERCIO' | 'CONSUMIDOR' | 'ORGANIZACION' | 'ADMINISTRADOR';
export type EstadoCuenta = 'PENDIENTE' | 'VALIDADA' | 'RECHAZADA';

export interface UsuarioVista {
  id: number;
  correo: string;
  nombre: string;
  rol: Rol;
  estadoCuenta?: EstadoCuenta;
}

export interface RespuestaAutenticacion {
  token: string;
  expiraEn: string;
  usuario: UsuarioVista;
}

export interface RegistroRequest {
  correo: string;
  clave: string;
  rol: Rol;
  nombre: string;
  ruc?: string;
  direccion?: string;
  distrito?: string;
  latitud?: number;
  longitud?: number;
  capacidadDiariaKg?: number;
}

export interface Comercio {
  id: number;
  nombre: string;
  ruc: string;
  direccion: string;
  distrito?: string;
  latitud: number;
  longitud: number;
  estado: EstadoCuenta;
  plan: 'GRATIS' | 'PREMIUM';
  motivoRechazo?: string;
}

export interface Organizacion {
  id: number;
  nombre: string;
  direccion: string;
  latitud: number;
  longitud: number;
  capacidadDiariaKg?: number;
  estado: EstadoCuenta;
  deducibleSunat: boolean;
  motivoRechazo?: string;
}

export interface OfertaVista {
  id: number;
  comercioId: number;
  comercioNombre: string;
  comercioDireccion: string;
  latitud: number;
  longitud: number;
  distanciaKm: number;
  titulo: string;
  descripcion?: string;
  categoria: string;
  cantidad: number;
  pesoUnitarioKg: number;
  precioOriginal: number;
  precioDescuento: number;
  venceEl: string;
  recojoDesde: string;
  recojoHasta: string;
}

export interface Oferta {
  id: number;
  titulo: string;
  categoria: string;
  cantidad: number;
  cantidadInicial: number;
  precioOriginal: number;
  precioDescuento: number;
  venceEl: string;
  recojoDesde: string;
  recojoHasta: string;
  estado: 'ACTIVA' | 'AGOTADA' | 'DONADA' | 'VENCIDA' | 'PERDIDA';
}

export interface CrearOferta {
  titulo: string;
  descripcion?: string;
  categoria: string;
  cantidad: number;
  pesoUnitarioKg: number;
  precioOriginal: number;
  precioDescuento: number;
  venceEl: string;
  recojoDesde: string;
  recojoHasta: string;
}

export interface ReservaVista {
  id: number;
  codigo: string;
  estado: 'PAGADA' | 'RECOGIDA' | 'NO_RECOGIDA';
  cantidad: number;
  monto: number;
  ofertaId: number;
  ofertaTitulo: string;
  comercioNombre: string;
  comercioDireccion: string;
  recojoDesde: string;
  recojoHasta: string;
  creadoEn: string;
}

export interface DonacionVista {
  id: number;
  codigo: string;
  estado: 'OFRECIDA' | 'ACEPTADA' | 'COMPLETADA' | 'RECHAZADA';
  ofertaTitulo: string;
  categoria: string;
  cantidad: number;
  pesoKg: number;
  comercioNombre: string;
  comercioDireccion: string;
  distanciaKm: number;
  recojoDesde: string;
  recojoHasta: string;
  completadaEn?: string;
  organizacionNombre: string;
}

export interface FilaRegistro {
  fecha: string;
  donante: string;
  donanteRuc: string;
  tipoAlimento: string;
  cantidad: number;
  pesoKg: number;
  beneficiario: string;
  deducible: boolean;
  codigo: string;
}

export interface Distintivo { otorgado: boolean; minKg: number; avancePct: number; mensaje: string; }

export interface ImpactoComercio {
  vendidoKg: number; donadoKg: number; rescatadoKg: number; perdidoKg: number; ingresos: number;
  co2EvitadoKg: number; donacionesCompletadas: number; plan: string; distintivo: Distintivo;
}

export interface ImpactoPlataforma {
  rescatadoKg: number; vendidoKg: number; donadoKg: number; co2EvitadoKg: number;
  comerciosActivos: number; organizacionesActivas: number; metaKg: number; avanceMetaPct: number;
}

export interface ImpactoOrganizacion { recibidoKg: number; donacionesCompletadas: number; co2EvitadoKg: number; }

export interface Prediccion {
  disponible: boolean;
  mensaje: string;
  fechaObjetivo: string;
  diasHistorial: number;
  predicciones: { categoria: string; unidadesEsperadas: number; descuentoSugeridoPct: number }[];
}

export interface Notificacion { id: number; titulo: string; mensaje: string; leida: boolean; creadoEn: string; }
export interface Notificaciones { sinLeer: number; items: Notificacion[]; }
