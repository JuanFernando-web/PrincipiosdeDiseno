package com.udla.arquitectura.demo.repository;

import com.udla.arquitectura.demo.exception.ProductoNoEncontradoException;
import com.udla.arquitectura.demo.model.Producto;
import com.udla.arquitectura.demo.observabilidad.MetricasPeticion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba unitaria del proxy: sin Spring, con un repositorio falso que cuenta
 * cuantas veces se le consulta.
 */
class ProductoRepositoryCacheProxyTest {

    /** Sujeto real falso: instantaneo y contable. */
    static class RepositorioFalso implements ProductoRepository {
        int llamadasLista = 0;
        int llamadasBusqueda = 0;

        @Override
        public List<Producto> listarTodos() {
            llamadasLista++;
            return List.of(new Producto(1L, "A", "X", 1.0), new Producto(2L, "B", "X", 2.0));
        }

        @Override
        public Producto buscarPorId(Long id) {
            llamadasBusqueda++;
            if (id != 1L) {
                throw new ProductoNoEncontradoException(id);
            }
            return new Producto(1L, "A", "X", 1.0);
        }
    }

    private RepositorioFalso real;
    private MetricasPeticion metricas;
    private ProductoRepositoryCacheProxy proxy;

    @BeforeEach
    void preparar() {
        real = new RepositorioFalso();
        metricas = new MetricasPeticion();
        proxy = new ProductoRepositoryCacheProxy(real, new ConcurrentMapCacheManager(), metricas);
    }

    @Test
    void listarTodos_primeraVezEsMiss_segundaVezEsHitYNoTocaElRepositorioReal() {
        proxy.listarTodos();
        assertEquals("MISS", metricas.estadoCache());

        List<Producto> segunda = proxy.listarTodos();
        assertEquals("HIT", metricas.estadoCache());
        assertEquals(2, segunda.size());
        assertEquals(1, real.llamadasLista, "el repositorio real debe consultarse una sola vez");
    }

    @Test
    void buscarPorId_cacheaPorClave() {
        proxy.buscarPorId(1L);
        proxy.buscarPorId(1L);
        assertEquals(1, real.llamadasBusqueda);
        assertEquals("HIT", metricas.estadoCache());
    }

    @Test
    void buscarPorId_inexistente_lanzaExcepcionYNoCachea() {
        assertThrows(ProductoNoEncontradoException.class, () -> proxy.buscarPorId(99L));
        assertThrows(ProductoNoEncontradoException.class, () -> proxy.buscarPorId(99L));
        assertEquals(2, real.llamadasBusqueda, "los errores no se guardan en cache");
    }
}
