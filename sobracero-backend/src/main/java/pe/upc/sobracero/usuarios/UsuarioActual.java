package pe.upc.sobracero.usuarios;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Acceso al id del usuario autenticado (claim "sub" del JWT). */
public final class UsuarioActual {
    private UsuarioActual() {}

    public static Long id() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return Long.valueOf(jwt.getSubject());
    }
}
