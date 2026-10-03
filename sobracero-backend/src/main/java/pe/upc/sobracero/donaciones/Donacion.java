package pe.upc.sobracero.donaciones;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donacion")
@Getter
@Setter
public class Donacion {

    public static final String OFRECIDA = "OFRECIDA";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String COMPLETADA = "COMPLETADA";
    public static final String RECHAZADA = "RECHAZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ofertaId;
    private Long organizacionId;
    private String codigo;
    private String estado = OFRECIDA;
    private int cantidad;
    private BigDecimal pesoKg = BigDecimal.ZERO;
    private LocalDateTime completadaEn;

    @Column(insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
