package pe.upc.sobracero.organizaciones;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.upc.sobracero.comun.EstadoCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizacion_social")
@Getter
@Setter
public class OrganizacionSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    private String nombre;
    private String direccion;
    private double latitud;
    private double longitud;

    /** Capacidad de recojo diaria en kg (US14). Nulo = sin límite declarado. */
    private BigDecimal capacidadDiariaKg;

    @Enumerated(EnumType.STRING)
    private EstadoCuenta estado = EstadoCuenta.PENDIENTE;

    /** US19: calificada como entidad perceptora ante SUNAT (donaciones deducibles). */
    private boolean deducibleSunat;

    private String motivoRechazo;

    @Column(insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
