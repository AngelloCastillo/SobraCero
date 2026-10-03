package pe.upc.sobracero;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Reloj controlable para probar recordatorios, derivación a donación y vencimientos. */
public class RelojModificable extends Clock {
    private final ZoneId zona;
    private volatile Instant instante;

    public RelojModificable(ZoneId zona, LocalDateTime inicio) {
        this.zona = zona;
        this.instante = inicio.atZone(zona).toInstant();
    }

    public void fijar(LocalDateTime t) {
        this.instante = t.atZone(zona).toInstant();
    }

    @Override public ZoneId getZone() { return zona; }
    @Override public Clock withZone(ZoneId z) { return new RelojModificable(z, LocalDateTime.ofInstant(instante, z)); }
    @Override public Instant instant() { return instante; }
}
