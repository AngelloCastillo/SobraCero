package pe.upc.sobracero.comun;

import java.security.SecureRandom;

/** Códigos cortos y legibles (sin caracteres ambiguos como 0/O o 1/I). */
public final class GeneradorCodigo {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private GeneradorCodigo() {}

    public static String generar(String prefijo, int largo) {
        StringBuilder sb = new StringBuilder(prefijo);
        for (int i = 0; i < largo; i++) sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        return sb.toString();
    }
}
