-- Esquema de la base de datos (PostgreSQL). Se ejecuta al iniciar el backend;
-- "IF NOT EXISTS" permite correrlo cada vez sin borrar datos.
--
-- Parte relacional: la definición de las encuestas (encuesta -> sección ->
-- pregunta -> opción), con claves foráneas para que el editor no deje datos
-- huérfanos. Parte documental: cada respuesta es un documento JSONB, porque las
-- respuestas son dispersas (ramas condicionales), tienen listas (elección
-- múltiple) y cambian de forma cuando se editan las preguntas.

CREATE TABLE IF NOT EXISTS encuesta (
    id           SERIAL PRIMARY KEY,
    codigo       VARCHAR(30)  NOT NULL UNIQUE,          -- 'titulados' | 'empleadores'
    nombre       VARCHAR(200) NOT NULL,
    introduccion TEXT,
    version      INT          NOT NULL DEFAULT 1,       -- sube con cada cambio de estructura
    activa       BOOLEAN      NOT NULL DEFAULT TRUE,
    creada       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS seccion (
    id          SERIAL PRIMARY KEY,
    encuesta_id INT          NOT NULL REFERENCES encuesta(id) ON DELETE RESTRICT,
    orden       INT          NOT NULL,
    titulo      VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS pregunta (
    id              SERIAL PRIMARY KEY,
    encuesta_id     INT          NOT NULL REFERENCES encuesta(id) ON DELETE RESTRICT,
    seccion_id      INT          NOT NULL REFERENCES seccion(id)  ON DELETE RESTRICT,
    codigo          VARCHAR(60)  NOT NULL,                -- nombre que usa el análisis
    texto           TEXT         NOT NULL,
    ayuda           TEXT,
    tipo            VARCHAR(20)  NOT NULL
                    CHECK (tipo IN ('unica', 'multiple', 'numero', 'texto', 'escala', 'email', 'telefono')),
    obligatoria     BOOLEAN      NOT NULL DEFAULT FALSE,
    orden           INT          NOT NULL,
    depende_de      VARCHAR(60),                          -- código de la pregunta que la habilita
    depende_valores TEXT,                                 -- valores que la habilitan, separados por coma
    max_selecciones INT,                                  -- tope en elección múltiple
    valor_minimo    NUMERIC,
    valor_maximo    NUMERIC,
    es_contacto     BOOLEAN      NOT NULL DEFAULT FALSE,  -- dato personal: va a la tabla contacto
    activa          BOOLEAN      NOT NULL DEFAULT TRUE,   -- desactivar en vez de borrar si tiene respuestas
    UNIQUE (encuesta_id, codigo)
);

CREATE TABLE IF NOT EXISTS opcion (
    id          SERIAL PRIMARY KEY,
    pregunta_id INT         NOT NULL REFERENCES pregunta(id) ON DELETE RESTRICT,
    codigo      VARCHAR(80) NOT NULL,
    etiqueta    TEXT        NOT NULL,
    orden       INT         NOT NULL,
    activa      BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (pregunta_id, codigo)
);

CREATE TABLE IF NOT EXISTS respuesta (
    id          BIGSERIAL PRIMARY KEY,
    encuesta_id INT         NOT NULL REFERENCES encuesta(id) ON DELETE RESTRICT,
    version     INT         NOT NULL,                    -- versión de la encuesta respondida
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now(),
    origen      VARCHAR(12) NOT NULL CHECK (origen IN ('formulario', 'importado', 'prueba')),
    datos       JSONB       NOT NULL                     -- { "codigo_pregunta": valor | [valores] }
);

-- Filtrar por encuesta y período es la consulta más frecuente: O(log N + M)
CREATE INDEX IF NOT EXISTS respuesta_encuesta_fecha ON respuesta (encuesta_id, fecha);

-- Datos personales separados de las respuestas: nunca entran al análisis
CREATE TABLE IF NOT EXISTS contacto (
    respuesta_id BIGINT PRIMARY KEY REFERENCES respuesta(id) ON DELETE CASCADE,
    email        VARCHAR(200),
    telefono     VARCHAR(50),
    linkedin     VARCHAR(300)
);
