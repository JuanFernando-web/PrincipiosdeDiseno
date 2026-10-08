package com.udla.arquitectura.demo.repository;

import com.udla.arquitectura.demo.model.Producto;
import com.udla.arquitectura.demo.observabilidad.MetricasPeticion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * PATRON PROXY (proxy de cache).
 *
 * <pre>
 *   ProductoService --> ProductoRepository (interfaz)
 *                            ^                ^
 *                            |                |
 *          ProductoRepositoryCacheProxy   RepositorioProductoEnMemoria
 *             (este, @Primary)              (sujeto real, lento)
 * </pre>
 *
 * Implementa la MISMA interfaz que el repositorio real, por lo que el
 * cliente ({@code ProductoService}) no cambia ni se entera de que existe:
 * Principio de Sustitucion de Liskov + Abierto/Cerrado + Inversion de
 * Dependencias.
 *
 * Responsabilidad unica: decidir si se responde desde la cache o si se
 * delega en el repositorio real. El repositorio real queda sin logica de
 * cache y la cache queda sin logica de negocio.
 */
@Primary
@Component
public class ProductoRepositoryCacheProxy implements ProductoRepository {

    static final String NOMBRE_CACHE = "productos";
    private static final String CLAVE_LISTA = "todos";

    private static final Logger log = LoggerFactory.getLogger(ProductoRepositoryCacheProxy.class);

    private final ProductoRepository real;
    private final CacheManager cacheManager;
    private final MetricasPeticion metricas;

    public ProductoRepositoryCacheProxy(
            @Qualifier("repositorioProductoEnMemoria") ProductoRepository real,
            CacheManager cacheManager,
            MetricasPeticion metricas) {
        this.real = real;
        this.cacheManager = cacheManager;
        this.metricas = metricas;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Producto> listarTodos() {
        Cache cache = cache();
        List<Producto> enCache = cache.get(CLAVE_LISTA, List.class);
        if (enCache != null) {
            registrarHit("listarTodos");
            return enCache;
        }
        registrarMiss("listarTodos");
        // copia inmutable: nadie puede alterar lo que esta guardado en cache
        List<Producto> productos = List.copyOf(real.listarTodos());
        cache.put(CLAVE_LISTA, productos);
        return productos;
    }

    @Override
    public Producto buscarPorId(Long id) {
        Cache cache = cache();
        String clave = "id:" + id;
        Producto enCache = cache.get(clave, Producto.class);
        if (enCache != null) {
            registrarHit("buscarPorId(" + id + ")");
            return enCache;
        }
        registrarMiss("buscarPorId(" + id + ")");
        // si el real lanza ProductoNoEncontradoException, no se cachea nada
        Producto producto = real.buscarPorId(id);
        cache.put(clave, producto);
        return producto;
    }

    private Cache cache() {
        return Objects.requireNonNull(cacheManager.getCache(NOMBRE_CACHE),
                "No existe la cache '" + NOMBRE_CACHE + "'");
    }

    private void registrarHit(String operacion) {
        metricas.registrarCache(true);
        log.info("CACHE HIT  -> {}", operacion);
    }

    private void registrarMiss(String operacion) {
        metricas.registrarCache(false);
        log.info("CACHE MISS -> {} (consultando repositorio real...)", operacion);
    }
}
