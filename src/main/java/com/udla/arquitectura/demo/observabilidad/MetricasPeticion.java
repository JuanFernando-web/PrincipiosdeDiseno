package com.udla.arquitectura.demo.observabilidad;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Datos de medicion de UNA peticion HTTP (un objeto nuevo por peticion).
 * Lo alimentan el interceptor (inicio) y el proxy de cache (HIT/MISS);
 * lo lee {@link CabecerasMetricasAdvice} para publicar las cabeceras.
 */
@Component
@RequestScope
public class MetricasPeticion {

    private long inicioNanos = System.nanoTime();
    private Boolean cacheHit; // null = la peticion no consulto la cache

    public void iniciar() {
        this.inicioNanos = System.nanoTime();
    }

    public double transcurridoMs() {
        return (System.nanoTime() - inicioNanos) / 1_000_000.0;
    }

    public void registrarCache(boolean hit) {
        this.cacheHit = hit;
    }

    public String estadoCache() {
        if (cacheHit == null) {
            return "N/A";
        }
        return cacheHit ? "HIT" : "MISS";
    }
}
