package pe.upc.sobracero.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.upc.sobracero.comun.Rol;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String correo;
    private String claveHash;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private String nombreMostrado = "";

    @Column(insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
