package pe.upc.sobracero.notificaciones;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    private String titulo;
    private String mensaje;
    private boolean leida;

    @Column(insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
