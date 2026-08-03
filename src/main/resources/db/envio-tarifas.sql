-- =============================================================================
--  AgroEnvíos — Tarifas de envío
--  Base de datos: la PRIMARIA (clientes)
-- =============================================================================
--  Fórmula que aplica EnvioService:
--
--    rango    = tramo de tarifas_envio_rango donde cae la distancia
--               (radio_inicial_km <= distancia_km <= radio_final_km)
--
--    subtotal = rango.tarifa_base
--             + distancia_km   * rango.costo_por_km
--             + tiempo_minutos * rango.costo_por_minuto
--
--    tarifa   = subtotal * (multiplicadores de las geocercas del destino)
--             + (recargos fijos de esas geocercas)
--
--    tarifa   = min(tarifa, tarifa_maxima)
--
--  Ejemplo: rango 0–10 km, base $120, $20/km → entrega de 9 km = 120 + (9 x 20) = $300
-- =============================================================================


-- ── 0. Solo si YA arrancaste el backend con la versión anterior ──────────────
--  configuracion_envio quedó con columnas viejas NOT NULL que romperían los
--  INSERT de abajo. Hay dos formas de limpiarlo; elige una.
--
--  OPCIÓN A — quitar solo las columnas que sobran (no pierdes nada más).
--  Revisa antes qué columnas tienes:
--
--     SELECT column_name, column_type, is_nullable
--     FROM information_schema.columns
--     WHERE table_schema = DATABASE() AND table_name = 'configuracion_envio'
--     ORDER BY ordinal_position;
--
--  MySQL no soporta DROP COLUMN IF EXISTS: si alguna no existe, el ALTER
--  completo falla sin aplicar nada. Quita esa línea y vuelve a correrlo.
--
-- ALTER TABLE configuracion_envio
--     DROP COLUMN tarifa_base,
--     DROP COLUMN precio_por_km,
--     DROP COLUMN precio_por_minuto,
--     DROP COLUMN km_incluidos,
--     DROP COLUMN minutos_incluidos,
--     DROP COLUMN tarifa_minima,
--     DROP COLUMN distancia_maxima_km;
--
--  Deben quedar: id, nombre, activa, origen_latitud, origen_longitud,
--                tarifa_maxima, created_at, updated_at.
--
--  OPCIÓN B — tirar las tablas y dejar que el backend las recree al reiniciar.
--  Solo contienen configuración (sin pedidos ni datos de clientes).
--
-- DROP TABLE IF EXISTS geocerca_envio_puntos;
-- DROP TABLE IF EXISTS geocercas_envio;
-- DROP TABLE IF EXISTS tarifas_envio_rango;
-- DROP TABLE IF EXISTS configuracion_envio;


-- ── 1. Tablas ────────────────────────────────────────────────────────────────
--  Si el backend ya arrancó con esta versión, las tablas existen y estos
--  CREATE no hacen nada.

CREATE TABLE IF NOT EXISTS configuracion_envio (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(255) NOT NULL,
    activa          BIT(1)       NOT NULL,
    origen_latitud  DOUBLE       NOT NULL,
    origen_longitud DOUBLE       NOT NULL,
    tarifa_maxima   DECIMAL(10,2)    NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tarifas_envio_rango (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    configuracion_id BIGINT        NOT NULL,
    nombre           VARCHAR(255)      NULL,
    radio_inicial_km DECIMAL(10,2) NOT NULL,
    radio_final_km   DECIMAL(10,2) NOT NULL,
    tarifa_base      DECIMAL(10,2) NOT NULL,
    costo_por_km     DECIMAL(10,2) NOT NULL,
    costo_por_minuto DECIMAL(10,2) NOT NULL,
    activa           BIT(1)        NOT NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_rango_configuracion (configuracion_id),
    CONSTRAINT fk_rango_configuracion FOREIGN KEY (configuracion_id)
        REFERENCES configuracion_envio (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS geocercas_envio (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(255)  NOT NULL,
    tipo            VARCHAR(20)   NOT NULL,
    multiplicador   DECIMAL(6,3)  NOT NULL,
    recargo_fijo    DECIMAL(10,2) NOT NULL,
    prioridad       INT           NOT NULL,
    activa          BIT(1)        NOT NULL,
    exclusiva       BIT(1)        NOT NULL,
    bloquea_envio   BIT(1)        NOT NULL,
    centro_latitud  DOUBLE            NULL,
    centro_longitud DOUBLE            NULL,
    radio_metros    DOUBLE            NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS geocerca_envio_puntos (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    geocerca_id  BIGINT NOT NULL,
    orden        INT    NOT NULL,
    latitud      DOUBLE NOT NULL,
    longitud     DOUBLE NOT NULL,
    PRIMARY KEY (id),
    KEY idx_punto_geocerca (geocerca_id),
    CONSTRAINT fk_punto_geocerca FOREIGN KEY (geocerca_id)
        REFERENCES geocercas_envio (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ── 2. Tarifario ─────────────────────────────────────────────────────────────
--  Solo una fila activa a la vez; se usa la activa con el id más alto.

UPDATE configuracion_envio SET activa = 0, updated_at = NOW(6) WHERE activa = 1;

INSERT INTO configuracion_envio
    (nombre, activa, origen_latitud, origen_longitud, tarifa_maxima, created_at, updated_at)
VALUES
    ('Tarifario 2026', 1, 21.912514787943113, -102.2945347636494, NULL, NOW(6), NOW(6));

SET @config_id = LAST_INSERT_ID();


-- ── 3. Rangos de radio ───────────────────────────────────────────────────────
--  costo_por_km se aplica sobre la distancia TOTAL de la ruta.
--  En la frontera exacta (10 km entre 0–10 y 10–25) gana el rango de menor radio.
--  Si la distancia no cae en ningún rango, no hay cobertura.

INSERT INTO tarifas_envio_rango
    (configuracion_id, nombre, radio_inicial_km, radio_final_km,
     tarifa_base, costo_por_km, costo_por_minuto, activa, created_at, updated_at)
VALUES
    (@config_id, 'Local',         0.00, 10.00, 120.00, 20.00, 0.00, 1, NOW(6), NOW(6)),
    (@config_id, 'Metropolitana', 10.00, 25.00, 180.00, 18.00, 0.00, 1, NOW(6), NOW(6)),
    (@config_id, 'Foránea',       25.00, 60.00, 250.00, 15.00, 0.00, 1, NOW(6), NOW(6));

--  9 km → 120 + (9  x 20) = $300
-- 15 km → 180 + (15 x 18) = $450
-- 40 km → 250 + (40 x 15) = $850


-- ── 4. Geocercas multiplicadoras (OPCIONAL) ──────────────────────────────────
--  Se evalúan contra la coordenada de entrega y se aplican SOBRE el resultado
--  del rango. Sin filas aquí, la tarifa es la del rango tal cual.
--
--  AJUSTA LAS COORDENADAS antes de descomentar: como están, son de ejemplo y
--  cambiarían precios reales.
--
--    multiplicador : 1.300 = +30%   |   recargo_fijo : $ extra
--    prioridad     : mayor primero, decide cuál gana si hay una exclusiva
--    exclusiva     : si aplica, ignora al resto de geocercas que coincidan
--    bloquea_envio : zona sin cobertura, la cotización falla con 400

-- Círculo: recargo por tráfico en el centro (+30%)
-- INSERT INTO geocercas_envio
--     (nombre, tipo, multiplicador, recargo_fijo, prioridad, activa, exclusiva, bloquea_envio,
--      centro_latitud, centro_longitud, radio_metros, created_at, updated_at)
-- VALUES
--     ('Centro histórico', 'CIRCULO', 1.300, 0.00, 10, 1, 0, 0,
--      21.8818, -102.2916, 2500, NOW(6), NOW(6));   -- radio en METROS

-- Círculo: zona rural, +50% y $80 fijos
-- INSERT INTO geocercas_envio
--     (nombre, tipo, multiplicador, recargo_fijo, prioridad, activa, exclusiva, bloquea_envio,
--      centro_latitud, centro_longitud, radio_metros, created_at, updated_at)
-- VALUES
--     ('Zona rural norte', 'CIRCULO', 1.500, 80.00, 5, 1, 0, 0,
--      22.0500, -102.3200, 15000, NOW(6), NOW(6));

-- Círculo: zona sin cobertura (corta la venta antes de cobrar)
-- INSERT INTO geocercas_envio
--     (nombre, tipo, multiplicador, recargo_fijo, prioridad, activa, exclusiva, bloquea_envio,
--      centro_latitud, centro_longitud, radio_metros, created_at, updated_at)
-- VALUES
--     ('Zona no cubierta', 'CIRCULO', 1.000, 0.00, 100, 1, 0, 1,
--      22.4000, -102.6000, 20000, NOW(6), NOW(6));

-- Polígono: la geocerca y luego sus vértices (el contorno se cierra solo,
-- no repitas el primer vértice)
-- INSERT INTO geocercas_envio
--     (nombre, tipo, multiplicador, recargo_fijo, prioridad, activa, exclusiva, bloquea_envio,
--      created_at, updated_at)
-- VALUES
--     ('Parque industrial', 'POLIGONO', 1.150, 0.00, 8, 1, 0, 0, NOW(6), NOW(6));
--
-- SET @geocerca_id = LAST_INSERT_ID();
--
-- INSERT INTO geocerca_envio_puntos (geocerca_id, orden, latitud, longitud) VALUES
--     (@geocerca_id, 1, 21.9300, -102.3100),
--     (@geocerca_id, 2, 21.9300, -102.2700),
--     (@geocerca_id, 3, 21.9000, -102.2700),
--     (@geocerca_id, 4, 21.9000, -102.3100);


-- ── 5. Verificación ──────────────────────────────────────────────────────────

SELECT c.id AS tarifario, c.nombre AS tarifario_nombre,
       r.nombre AS rango, r.radio_inicial_km, r.radio_final_km,
       r.tarifa_base, r.costo_por_km, r.costo_por_minuto
FROM configuracion_envio c
JOIN tarifas_envio_rango r ON r.configuracion_id = c.id
WHERE c.activa = 1 AND r.activa = 1
ORDER BY r.radio_inicial_km;


-- ── 6. Mantenimiento ─────────────────────────────────────────────────────────

-- Subir el precio por km de un tramo
-- UPDATE tarifas_envio_rango SET costo_por_km = 22.00, updated_at = NOW(6) WHERE id = 1;

-- Ampliar la cobertura agregando un tramo al tarifario activo
-- INSERT INTO tarifas_envio_rango
--     (configuracion_id, nombre, radio_inicial_km, radio_final_km,
--      tarifa_base, costo_por_km, costo_por_minuto, activa, created_at, updated_at)
-- SELECT id, 'Larga distancia', 60.00, 120.00, 400.00, 12.00, 0.00, 1, NOW(6), NOW(6)
-- FROM configuracion_envio WHERE activa = 1;

-- Apagar un rango o una geocerca sin borrarlos
-- UPDATE tarifas_envio_rango SET activa = 0, updated_at = NOW(6) WHERE id = 3;
-- UPDATE geocercas_envio     SET activa = 0, updated_at = NOW(6) WHERE nombre = 'Centro histórico';

-- Geocercas vigentes en orden de evaluación
-- SELECT id, nombre, tipo, multiplicador, recargo_fijo, prioridad, exclusiva, bloquea_envio
-- FROM geocercas_envio WHERE activa = 1 ORDER BY prioridad DESC, id ASC;
