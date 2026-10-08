package com.udla.arquitectura.demo.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Operaciones de administracion de la cache (vaciarla para repetir la demo).
 */
@Service
public class CacheAdminService {

    private final CacheManager cacheManager;

    public CacheAdminService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void limpiarTodo() {
        for (String nombre : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(nombre);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
