# ISWZ2202 — Catálogo de productos (Caché + Patrón Proxy)

App de catálogo de productos (Spring Boot 3 / Java 17). El repositorio simula una
base de datos lenta (**1500 ms por consulta**); se añadió una **caché** mediante el
**patrón Proxy** y un **frontend** que muestra el tiempo de respuesta de cada consulta.

> Enlace al repositorio GitHub: `https://github.com/<tu-usuario>/<tu-repo>`  ← completar

## Qué se implementó

| Requisito | Implementación |
|---|---|
| Completar la app | Excepción de dominio `ProductoNoEncontradoException` + `ManejadorErrores` (404 en vez de 500), endpoint `DELETE /api/cache`, pruebas |
| Caché | Spring Cache (`@EnableCaching`) con **Caffeine**: máx. 100 entradas, TTL 60 s (`application.properties`) |
| Patrón Proxy | `ProductoRepositoryCacheProxy` implementa `ProductoRepository` y envuelve al repositorio real |
| Tiempo de respuesta | Cabeceras `X-Response-Time-Ms` y `X-Cache` (HIT/MISS) en toda respuesta + panel en el frontend |
| Frontend + CORS | `frontend/index.html` (HTML/JS puro) y configuración CORS en `WebConfig` |

## Diseño

```
Controller ──> ProductoService ──> «interface» ProductoRepository
                                          ▲                  ▲
                                          │                  │
                       ProductoRepositoryCacheProxy   RepositorioProductoEnMemoria
                         (@Primary: Proxy + caché)       (sujeto real, 1500 ms)
                                   │  MISS ───────────────────┘
                                   └──> CacheManager (Caffeine)
```

- **El `ProductoService` no se modificó**: depende de la interfaz, y Spring le inyecta
  el proxy (`@Primary`). Agregar la caché no tocó ni al servicio ni al repositorio real
  (Abierto/Cerrado, Inversión de Dependencias, Liskov).
- **Responsabilidades separadas**: el repositorio real solo accede a datos; el proxy solo
  decide HIT/MISS; `observabilidad/` solo mide; `config/` solo configura.
- La lista cacheada se guarda como `List.copyOf(...)` (inmutable) para que ningún cliente
  pueda corromper el contenido de la caché.
- Las excepciones **no** se cachean (un producto inexistente se vuelve a consultar).
- Alternativa declarativa: anotar con `@Cacheable("productos")` también aplica un proxy
  (AOP generado por Spring). Aquí se escribió el proxy a mano para hacer visible el patrón GoF.

## Ejecutar el backend

```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```

API en `http://localhost:8080`:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos` | Lista todos |
| GET | `/api/productos/{id}` | Un producto (404 si no existe) |
| DELETE | `/api/cache` | Vacía la caché (para repetir la demo) |

## Demostrar la caché (tiempos de respuesta)

**Con curl** — `-w` imprime el tiempo total y `-i` muestra las cabeceras:

```bash
curl -s -o /dev/null -w "total: %{time_total}s\n" -D - http://localhost:8080/api/productos/1 | grep -iE "x-cache|x-response|total"
curl -s -o /dev/null -w "total: %{time_total}s\n" -D - http://localhost:8080/api/productos/1 | grep -iE "x-cache|x-response|total"
```

Resultado esperado: 1.ª vez `X-Cache: MISS` ≈ 1500 ms; 2.ª vez `X-Cache: HIT` ≈ 1–5 ms.
En la consola del servidor también se ve `CACHE MISS -> ...` / `CACHE HIT -> ...`.

**Con el frontend** (recomendado para la captura de pantalla de la entrega):

```bash
cd frontend
python3 -m http.server 5500      # Windows: py -m http.server 5500
```

Abrir `http://localhost:5500` y pulsar **▶ Demo automática**, o usar los botones
manualmente. El panel muestra el tiempo de la última respuesta, la etiqueta
HIT/MISS, el promedio con y sin caché y cuántas veces más rápido es.

## CORS: qué es y cómo se resolvió

**Causa.** El frontend (`http://localhost:5500`) y la API (`http://localhost:8080`) son
**orígenes distintos** (origen = protocolo + host + puerto). La *Same-Origin Policy* del
navegador impide que el JavaScript de una página lea respuestas de otro origen a menos
que ese servidor lo autorice con cabeceras `Access-Control-Allow-*`. Para peticiones
«no simples» el navegador primero envía un *preflight* (`OPTIONS`) preguntando si está
permitido. **Es una protección del navegador**: `curl` o Postman nunca la sufren.

**Soluciones en desarrollo:**

1. **Habilitar CORS en el backend** (la que usa este proyecto, ver `config/WebConfig.java`):
   solo para `/api/**`, con una lista explícita de orígenes en
   `app.cors.allowed-origins` (nunca `*` en producción) y con
   `exposedHeaders("X-Response-Time-Ms", "X-Cache")` — sin esto el navegador **oculta**
   esas cabeceras al JavaScript aunque lleguen.
2. **Proxy del servidor de desarrollo del frontend** (Vite/webpack/CRA): el navegador
   solo habla con su propio origen y el dev-server reenvía `/api` al backend, así que
   no hay cross-origin. Ejemplo `vite.config.js`:
   ```js
   export default { server: { proxy: { '/api': 'http://localhost:8080' } } }
   ```
3. **Servir el frontend desde Spring** (`src/main/resources/static/`): mismo origen,
   no hace falta CORS.

Para ver el error: quita `http://localhost:5500` de `app.cors.allowed-origins`, reinicia
y recarga el frontend; la consola mostrará *«blocked by CORS policy»*.

## Pruebas

```bash
./mvnw test
```

- `ProductoRepositoryCacheProxyTest`: unitaria del proxy (repositorio falso que cuenta
  llamadas): la 2.ª consulta no toca el repositorio real; los errores no se cachean.
- `ProductoApiIntegracionTest`: de punta a punta: MISS ≥ 1000 ms → HIT < 200 ms,
  vaciado de caché, 404, preflight CORS permitido y origen no autorizado rechazado.

## Entrega en GitHub

```bash
git init
git add .
git commit -m "Cache + patron Proxy + frontend"
git branch -M main
git remote add origin https://github.com/<tu-usuario>/<tu-repo>.git
git push -u origin main
```

Pega el enlace del repositorio en la consigna (y en la línea de arriba de este README).
