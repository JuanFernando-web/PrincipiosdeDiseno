package com.udla.arquitectura.demo.observabilidad;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Locale;

/**
 * Agrega a TODAS las respuestas de la API:
 *  - X-Response-Time-Ms: tiempo de proceso en el servidor (ms)
 *  - X-Cache: HIT | MISS | N/A
 *
 * Se hace aqui (y no en cada controller) para no repetir codigo ni ensuciar
 * los controllers con una preocupacion transversal.
 */
@RestControllerAdvice
public class CabecerasMetricasAdvice implements ResponseBodyAdvice<Object> {

    private final MetricasPeticion metricas;

    public CabecerasMetricasAdvice(MetricasPeticion metricas) {
        this.metricas = metricas;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().set("X-Response-Time-Ms",
                String.format(Locale.ROOT, "%.1f", metricas.transcurridoMs()));
        response.getHeaders().set("X-Cache", metricas.estadoCache());
        return body;
    }
}
