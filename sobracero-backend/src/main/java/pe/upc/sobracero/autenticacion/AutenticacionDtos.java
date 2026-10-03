package pe.upc.sobracero.autenticacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.upc.sobracero.comun.EstadoCuenta;
import pe.upc.sobracero.comun.Rol;

import java.math.BigDecimal;
import java.time.Instant;

public final class AutenticacionDtos {
    private AutenticacionDtos() {}

    public record RegistroRequest(
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String correo,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String clave,
            @NotNull(message = "El rol es obligatorio") Rol rol,
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            String ruc, String direccion, String distrito,
            Double latitud, Double longitud, BigDecimal capacidadDiariaKg) {}

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String clave) {}

    public record UsuarioVista(Long id, String correo, String nombre, Rol rol, EstadoCuenta estadoCuenta) {}

    public record RespuestaAutenticacion(String token, Instant expiraEn, UsuarioVista usuario) {}
}
