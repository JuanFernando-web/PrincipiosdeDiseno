package com.udla.arquitectura.demo.config;

import com.udla.arquitectura.demo.observabilidad.MetricasInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion web: CORS e interceptor de metricas.
 *
 * CORS: el navegador bloquea que una pagina servida desde un origen
 * (p. ej. http://localhost:5500) lea respuestas de otro origen
 * (http://localhost:8080) salvo que el servidor lo autorice con las
 * cabeceras Access-Control-*. Aqui se autoriza SOLO a los origenes listados
 * en la propiedad app.cors.allowed-origins (nunca "*" en produccion).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;
    private final MetricasInterceptor metricasInterceptor;

    public WebConfig(@Value("${app.cors.allowed-origins}") String[] origenesPermitidos,
                     MetricasInterceptor metricasInterceptor) {
        this.origenesPermitidos = origenesPermitidos;
        this.metricasInterceptor = metricasInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "DELETE", "OPTIONS")
                // Sin esto el JS del navegador NO puede leer cabeceras propias
                .exposedHeaders("X-Response-Time-Ms", "X-Cache")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricasInterceptor).addPathPatterns("/api/**");
    }
}
