package pe.upc.sobracero.autenticacion;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.sobracero.comercios.Comercio;
import pe.upc.sobracero.comercios.ComercioRepository;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.ExcepcionApi;
import pe.upc.sobracero.comun.Rol;
import pe.upc.sobracero.organizaciones.OrganizacionSocial;
import pe.upc.sobracero.organizaciones.OrganizacionSocialRepository;
import pe.upc.sobracero.usuarios.Usuario;
import pe.upc.sobracero.usuarios.UsuarioRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final UsuarioRepository usuarios;
    private final ComercioRepository comercios;
    private final OrganizacionSocialRepository organizaciones;
    private final PasswordEncoder codificador;
    private final JwtEncoder jwtEncoder;
    private final Clock reloj;

    @Value("${app.jwt.horas-expiracion}")
    private long horasExpiracion;

    @Transactional
    public AutenticacionDtos.RespuestaAutenticacion registrar(AutenticacionDtos.RegistroRequest req) {
        if (req.rol() == Rol.ADMINISTRADOR) throw ExcepcionApi.solicitudInvalida("Rol no permitido");
        if (usuarios.existsByCorreoIgnoreCase(req.correo())) {
            throw ExcepcionApi.conflicto("Ya existe una cuenta con ese correo");
        }
        Usuario u = new Usuario();
        u.setCorreo(req.correo().trim().toLowerCase());
        u.setClaveHash(codificador.encode(req.clave()));
        u.setRol(req.rol());
        u.setNombreMostrado(req.nombre().trim());
        u = usuarios.save(u);

        switch (req.rol()) {
            case COMERCIO -> {
                exigir(req.ruc() != null && req.ruc().matches("\\d{11}"), "El RUC debe tener 11 dígitos");
                exigirUbicacion(req);
                Comercio c = new Comercio();
                c.setUsuarioId(u.getId());
                c.setNombre(req.nombre().trim());
                c.setRuc(req.ruc());
                c.setDireccion(req.direccion());
                c.setDistrito(req.distrito());
                c.setLatitud(req.latitud());
                c.setLongitud(req.longitud());
                comercios.save(c);
            }
            case ORGANIZACION -> {
                exigirUbicacion(req);
                OrganizacionSocial o = new OrganizacionSocial();
                o.setUsuarioId(u.getId());
                o.setNombre(req.nombre().trim());
                o.setDireccion(req.direccion());
                o.setLatitud(req.latitud());
                o.setLongitud(req.longitud());
                o.setCapacidadDiariaKg(req.capacidadDiariaKg());
                organizaciones.save(o);
            }
            default -> { }
        }
        return respuesta(u);
    }

    @Transactional(readOnly = true)
    public AutenticacionDtos.RespuestaAutenticacion login(AutenticacionDtos.LoginRequest req) {
        Usuario u = usuarios.findByCorreoIgnoreCase(req.correo().trim())
                .filter(x -> codificador.matches(req.clave(), x.getClaveHash()))
                .orElseThrow(() -> new ExcepcionApi(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        return respuesta(u);
    }

    @Transactional(readOnly = true)
    public AutenticacionDtos.UsuarioVista yo(Long usuarioId) {
        return vista(usuarios.findById(usuarioId).orElseThrow(() -> ExcepcionApi.noEncontrado("Usuario no encontrado")));
    }

    private AutenticacionDtos.RespuestaAutenticacion respuesta(Usuario u) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(Duration.ofHours(horasExpiracion));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(u.getId()))
                .issuedAt(ahora).expiresAt(expira)
                .claim("rol", u.getRol().name())
                .claim("correo", u.getCorreo())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AutenticacionDtos.RespuestaAutenticacion(token, expira, vista(u));
    }

    private AutenticacionDtos.UsuarioVista vista(Usuario u) {
        EstadoCuenta estado = switch (u.getRol()) {
            case COMERCIO -> comercios.findByUsuarioId(u.getId()).map(Comercio::getEstado).orElse(null);
            case ORGANIZACION -> organizaciones.findByUsuarioId(u.getId()).map(OrganizacionSocial::getEstado).orElse(null);
            default -> null;
        };
        return new AutenticacionDtos.UsuarioVista(u.getId(), u.getCorreo(), u.getNombreMostrado(), u.getRol(), estado);
    }

    private static void exigir(boolean ok, String mensaje) {
        if (!ok) throw ExcepcionApi.solicitudInvalida(mensaje);
    }

    private static void exigirUbicacion(AutenticacionDtos.RegistroRequest r) {
        exigir(r.direccion() != null && !r.direccion().isBlank(), "La dirección es obligatoria");
        exigir(r.latitud() != null && r.longitud() != null, "La ubicación (latitud y longitud) es obligatoria");
    }
}
