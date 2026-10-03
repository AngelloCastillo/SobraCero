package pe.upc.sobracero.ofertas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "oferta")
@Getter
@Setter
public class Oferta {

    public static final String ACTIVA = "ACTIVA";
    public static final String AGOTADA = "AGOTADA";     // todas las unidades reservadas
    public static final String DONADA = "DONADA";       // derivada a donación
    public static final String VENCIDA = "VENCIDA";
    public static final String PERDIDA = "PERDIDA";     // pérdida no evitada

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long comercioId;
    private String titulo;
    private String descripcion;
    private String categoria;

    /** Unidades disponibles (baja con cada reserva). */
    private int cantidad;

    private int cantidadInicial;
    private BigDecimal pesoUnitarioKg = new BigDecimal("0.5");
    private BigDecimal precioOriginal;
    private BigDecimal precioDescuento;
    private LocalDate venceEl;
    private LocalDateTime recojoDesde;
    private LocalDateTime recojoHasta;

    private String estado = ACTIVA;

    @Column(insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
