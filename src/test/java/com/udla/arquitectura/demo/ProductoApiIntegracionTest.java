package com.udla.arquitectura.demo;

import com.udla.arquitectura.demo.service.CacheAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de punta a punta: controller -> service -> proxy -> repositorio real. */
@SpringBootTest
@AutoConfigureMockMvc
class ProductoApiIntegracionTest {

    private static final String ORIGEN_FRONT = "http://localhost:5500";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CacheAdminService cacheAdminService;

    @BeforeEach
    void vaciarCache() {
        cacheAdminService.limpiarTodo();
    }

    @Test
    void segundaConsultaSeSirveDesdeCacheYEsMuchoMasRapida() throws Exception {
        String ruta = "/api/productos/1";

        double msMiss = Double.parseDouble(mockMvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andReturn().getResponse().getHeader("X-Response-Time-Ms"));

        double msHit = Double.parseDouble(mockMvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"))
                .andReturn().getResponse().getHeader("X-Response-Time-Ms"));

        assertTrue(msMiss >= 1000, "el MISS debe pagar la latencia simulada, fue " + msMiss);
        assertTrue(msHit < 200, "el HIT debe ser casi instantaneo, fue " + msHit);
    }

    @Test
    void limpiarCacheVuelveAProducirMiss() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(header().string("X-Cache", "MISS"));
        mockMvc.perform(get("/api/productos")).andExpect(header().string("X-Cache", "HIT"));

        mockMvc.perform(delete("/api/cache")).andExpect(status().isOk());

        mockMvc.perform(get("/api/productos")).andExpect(header().string("X-Cache", "MISS"));
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/productos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void preflightCorsDesdeElFrontendEstaPermitido() throws Exception {
        mockMvc.perform(options("/api/productos")
                        .header("Origin", ORIGEN_FRONT)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_FRONT));
    }

    @Test
    void origenNoAutorizadoEsRechazado() throws Exception {
        mockMvc.perform(get("/api/productos/1").header("Origin", "http://sitio-malicioso.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cabecerasPropiasSeExponenAlNavegador() throws Exception {
        mockMvc.perform(get("/api/productos/2").header("Origin", ORIGEN_FRONT))
                .andExpect(header().string("Access-Control-Expose-Headers",
                        org.hamcrest.Matchers.containsString("X-Cache")));
    }
}
