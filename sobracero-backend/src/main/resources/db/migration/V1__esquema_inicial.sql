-- Esquema SobraCero (sin PostGIS: la cercanía se calcula con Haversine)

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    correo          VARCHAR(160) NOT NULL UNIQUE,
    clave_hash      VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL CHECK (rol IN ('COMERCIO','CONSUMIDOR','ORGANIZACION','ADMINISTRADOR')),
    nombre_mostrado VARCHAR(160) NOT NULL DEFAULT '',
    creado_en       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE comercio (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT REFERENCES usuario(id),
    nombre          VARCHAR(160) NOT NULL,
    ruc             VARCHAR(11)  NOT NULL UNIQUE,
    direccion       VARCHAR(255) NOT NULL,
    distrito        VARCHAR(80),
    latitud         DOUBLE PRECISION NOT NULL,
    longitud        DOUBLE PRECISION NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','VALIDADA','RECHAZADA')),
    plan            VARCHAR(10)  NOT NULL DEFAULT 'GRATIS' CHECK (plan IN ('GRATIS','PREMIUM')),
    motivo_rechazo  VARCHAR(255),
    creado_en       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE organizacion_social (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT REFERENCES usuario(id),
    nombre              VARCHAR(160) NOT NULL,
    direccion           VARCHAR(255) NOT NULL,
    latitud             DOUBLE PRECISION NOT NULL,
    longitud            DOUBLE PRECISION NOT NULL,
    capacidad_diaria_kg NUMERIC(10,2),
    estado              VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','VALIDADA','RECHAZADA')),
    deducible_sunat     BOOLEAN      NOT NULL DEFAULT FALSE,
    motivo_rechazo      VARCHAR(255),
    creado_en           TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE oferta (
    id               BIGSERIAL PRIMARY KEY,
    comercio_id      BIGINT       NOT NULL REFERENCES comercio(id),
    titulo           VARCHAR(160) NOT NULL,
    descripcion      VARCHAR(500),
    categoria        VARCHAR(40)  NOT NULL,
    cantidad         INTEGER      NOT NULL CHECK (cantidad >= 0),
    cantidad_inicial INTEGER      NOT NULL CHECK (cantidad_inicial > 0),
    peso_unitario_kg NUMERIC(8,3) NOT NULL DEFAULT 0.5 CHECK (peso_unitario_kg > 0),
    precio_original  NUMERIC(10,2) NOT NULL CHECK (precio_original > 0),
    precio_descuento NUMERIC(10,2) NOT NULL CHECK (precio_descuento >= 0),
    vence_el         DATE         NOT NULL,
    recojo_desde     TIMESTAMP    NOT NULL,
    recojo_hasta     TIMESTAMP    NOT NULL,
    estado           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','AGOTADA','DONADA','VENCIDA','PERDIDA')),
    creado_en        TIMESTAMP    NOT NULL DEFAULT now(),
    CHECK (recojo_hasta > recojo_desde)
);
CREATE INDEX idx_oferta_estado_recojo_hasta ON oferta(estado, recojo_hasta);
CREATE INDEX idx_oferta_comercio ON oferta(comercio_id);

CREATE TABLE reserva (
    id                   BIGSERIAL PRIMARY KEY,
    oferta_id            BIGINT      NOT NULL REFERENCES oferta(id),
    consumidor_id        BIGINT      NOT NULL REFERENCES usuario(id),
    codigo               VARCHAR(12) NOT NULL UNIQUE,
    estado               VARCHAR(20) NOT NULL DEFAULT 'PAGADA' CHECK (estado IN ('PAGADA','RECOGIDA','NO_RECOGIDA')),
    monto                NUMERIC(10,2) NOT NULL,
    cantidad             INTEGER     NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    recordatorio_enviado BOOLEAN     NOT NULL DEFAULT FALSE,
    recogida_en          TIMESTAMP,
    creado_en            TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_reserva_oferta ON reserva(oferta_id);
CREATE INDEX idx_reserva_consumidor ON reserva(consumidor_id);

CREATE TABLE donacion (
    id              BIGSERIAL PRIMARY KEY,
    oferta_id       BIGINT      NOT NULL REFERENCES oferta(id),
    organizacion_id BIGINT      NOT NULL REFERENCES organizacion_social(id),
    codigo          VARCHAR(12) NOT NULL UNIQUE,
    estado          VARCHAR(20) NOT NULL DEFAULT 'OFRECIDA' CHECK (estado IN ('OFRECIDA','ACEPTADA','COMPLETADA','RECHAZADA')),
    cantidad        INTEGER     NOT NULL DEFAULT 1,
    peso_kg         NUMERIC(10,3) NOT NULL DEFAULT 0,
    completada_en   TIMESTAMP,
    creado_en       TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_donacion_oferta ON donacion(oferta_id);
CREATE INDEX idx_donacion_organizacion ON donacion(organizacion_id);

-- Notificaciones dentro de la app (avisos de donación, recordatorios, etc.)
CREATE TABLE notificacion (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuario(id),
    titulo     VARCHAR(160) NOT NULL,
    mensaje    VARCHAR(500) NOT NULL,
    leida      BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_notificacion_usuario ON notificacion(usuario_id, creado_en DESC);
