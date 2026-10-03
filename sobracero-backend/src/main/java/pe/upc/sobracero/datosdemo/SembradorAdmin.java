package pe.upc.sobracero.datosdemo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.upc.sobracero.comun.Rol;
import pe.upc.sobracero.usuarios.Usuario;
import pe.upc.sobracero.usuarios.UsuarioRepository;

/** Crea la cuenta de administrador si no existe (credenciales por configuración). */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SembradorAdmin implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    @Value("${app.admin.correo:}")
    private String correo;

    @Value("${app.admin.clave:}")
    private String clave;

    @Override
    public void run(ApplicationArguments args) {
        if (correo.isBlank() || clave.isBlank() || usuarios.existsByCorreoIgnoreCase(correo)) return;
        Usuario admin = new Usuario();
        admin.setCorreo(correo.toLowerCase());
        admin.setClaveHash(codificador.encode(clave));
        admin.setRol(Rol.ADMINISTRADOR);
        admin.setNombreMostrado("Administrador");
        usuarios.save(admin);
        log.info("Administrador creado: {}", correo);
    }
}
