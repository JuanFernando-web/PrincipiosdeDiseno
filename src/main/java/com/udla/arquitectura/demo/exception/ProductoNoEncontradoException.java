package com.udla.arquitectura.demo.exception;

/**
 * Error de dominio: el producto solicitado no existe.
 * Se traduce a HTTP 404 en {@code ManejadorErrores}.
 */
public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Long id) {
        super("Producto no encontrado: " + id);
    }
}
