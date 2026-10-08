package com.udla.arquitectura.demo.controller;

import com.udla.arquitectura.demo.service.CacheAdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint para vaciar la cache y poder repetir la demostracion MISS -> HIT.
 */
@RestController
public class CacheController {

    private final CacheAdminService cacheAdminService;

    public CacheController(CacheAdminService cacheAdminService) {
        this.cacheAdminService = cacheAdminService;
    }

    @DeleteMapping("/api/cache")
    public Map<String, String> limpiar() {
        cacheAdminService.limpiarTodo();
        return Map.of("mensaje", "Cache vaciada");
    }
}
