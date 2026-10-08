package com.udla.arquitectura.demo.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Activa la infraestructura de cache de Spring. Con esto Spring Boot crea el
 * {@code CacheManager} (Caffeine, configurado en application.properties) que
 * usa el proxy del repositorio.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
