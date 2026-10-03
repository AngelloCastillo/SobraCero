package pe.upc.sobracero.reservas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
@Getter
@Setter
public class Reserva {

    public static final String PAGADA = "PAGADA";
    public static final String RECOGIDA = "RECOGIDA";
    public static final String NO_RECOGIDA = "NO_RECOGIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ofertaId;
    private Long consumidorId;
    private String codigo;
    private String estado = PAGADA;
    private BigDecimal monto;
    private int cantidad = 1;
    private boolean recordatorioEnviado;
    private LocalDateTime recogidaEn;

    @Column(insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
