package com.udla.arquitectura.demo.observabilidad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Marca el instante en que empieza a procesarse la peticion.
 */
@Component
public class MetricasInterceptor implements HandlerInterceptor {

    private final MetricasPeticion metricas;

    public MetricasInterceptor(MetricasPeticion metricas) {
        this.metricas = metricas;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        metricas.iniciar();
        return true;
    }
}
