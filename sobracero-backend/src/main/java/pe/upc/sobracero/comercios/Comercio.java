package pe.upc.sobracero.comercios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.upc.sobracero.comun.EstadoCuenta;

import java.time.LocalDateTime;

@Entity
@Table(name = "comercio")
@Getter
@Setter
public class Comercio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    private String nombre;
    private String ruc;
    private String direccion;
    private String distrito;
    private double latitud;
    private double longitud;

    @Enumerated(EnumType.STRING)
    private EstadoCuenta estado = EstadoCuenta.PENDIENTE;

    /** GRATIS o PREMIUM (suscripción con módulo de predicción, reportes y distintivo). */
    private String plan = "GRATIS";

    private String motivoRechazo;

    @Column(insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
