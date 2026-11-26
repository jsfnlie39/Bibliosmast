-- =====================================================================
-- BIBLOSMAST - Script de base de datos (MySQL 8+)
-- Ejecutar en DBeaver: abrir como Script SQL y correr con Alt+X
-- =====================================================================
DROP DATABASE IF EXISTS biblosmast;
CREATE DATABASE biblosmast CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE biblosmast;

-- ---------------------------------------------------------------------
-- 1. SEGURIDAD: roles, usuarios (personas) y cuentas (login)
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    id_rol      INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(150)
) ENGINE=InnoDB;

-- Persona: datos personales (socios, bibliotecarios, administradores)
CREATE TABLE usuario (
    id_usuario     INT AUTO_INCREMENT PRIMARY KEY,
    documento      VARCHAR(20)  NOT NULL,
    nombres        VARCHAR(80)  NOT NULL,
    apellidos      VARCHAR(80)  NOT NULL,
    email          VARCHAR(120) NOT NULL,
    telefono       VARCHAR(20),
    estado         ENUM('ACTIVO','INACTIVO','SANCIONADO') NOT NULL DEFAULT 'ACTIVO',
    fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Baja logica: un usuario "eliminado" NO se borra. Conserva sus datos y todo su historial (prestamos,
    -- reservas, multas, auditoria), y su documento y correo quedan libres para volver a registrarse.
    eliminado      TINYINT(1)   NOT NULL DEFAULT 0,
    fecha_baja     DATETIME     NULL,
    -- Columnas calculadas: valen NULL cuando el usuario esta dado de baja, asi la restriccion UNIQUE
    -- solo aplica entre usuarios vigentes (MySQL permite varios NULL en un indice unico).
    documento_vigente VARCHAR(20)  GENERATED ALWAYS AS (IF(eliminado = 1, NULL, documento)) STORED,
    email_vigente     VARCHAR(120) GENERATED ALWAYS AS (IF(eliminado = 1, NULL, email)) STORED,
    UNIQUE KEY uq_usuario_documento (documento_vigente),
    UNIQUE KEY uq_usuario_email (email_vigente)
) ENGINE=InnoDB;

-- Cuenta: credenciales de acceso al sistema (login). Una persona = una cuenta.
CREATE TABLE cuenta (
    id_cuenta      INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario     INT          NOT NULL UNIQUE,
    username       VARCHAR(40)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,              -- BCrypt, nunca texto plano
    id_rol         INT          NOT NULL,
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_creacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    intentos_fallidos  INT      NOT NULL DEFAULT 0,        -- claves malas seguidas; al llegar al maximo se bloquea la cuenta
    bloqueado_hasta    DATETIME NULL,                      -- mientras no pase esta fecha, no se puede iniciar sesion
    debe_cambiar_clave TINYINT(1) NOT NULL DEFAULT 0,      -- 1 = clave temporal: debe cambiarla antes de usar el sistema
    token_desde        DATETIME NULL,                      -- los tokens emitidos antes de esta fecha ya no valen (cerrar sesion)
    eliminada          TINYINT(1) NOT NULL DEFAULT 0,      -- 1 = el usuario fue dado de baja: la cuenta no puede iniciar sesion
    username_vigente   VARCHAR(40) GENERATED ALWAYS AS (IF(eliminada = 1, NULL, username)) STORED,
    UNIQUE KEY uq_cuenta_username (username_vigente),
    CONSTRAINT fk_cuenta_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_cuenta_rol     FOREIGN KEY (id_rol)     REFERENCES rol(id_rol)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. CATALOGO: categorias, editoriales, autores, libros, ejemplares
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(60) NOT NULL UNIQUE,
    descripcion  VARCHAR(150)
) ENGINE=InnoDB;

CREATE TABLE editorial (
    id_editorial INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL UNIQUE,
    pais         VARCHAR(60)
) ENGINE=InnoDB;

CREATE TABLE autor (
    id_autor     INT AUTO_INCREMENT PRIMARY KEY,
    nombres      VARCHAR(80) NOT NULL,
    apellidos    VARCHAR(80) NOT NULL,
    nacionalidad VARCHAR(60)
) ENGINE=InnoDB;

CREATE TABLE libro (
    id_libro         INT AUTO_INCREMENT PRIMARY KEY,
    isbn             VARCHAR(20)  NOT NULL UNIQUE,
    titulo           VARCHAR(200) NOT NULL,
    id_categoria     INT NOT NULL,
    id_editorial     INT NOT NULL,
    anio_publicacion SMALLINT,
    edicion          VARCHAR(30),
    idioma           VARCHAR(30) DEFAULT 'Español',
    numero_paginas   INT,
    descripcion      TEXT,
    estado           ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT fk_libro_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria),
    CONSTRAINT fk_libro_editorial FOREIGN KEY (id_editorial) REFERENCES editorial(id_editorial),
    INDEX idx_libro_titulo (titulo)
) ENGINE=InnoDB;

-- Relacion N:M libro - autor
CREATE TABLE libro_autor (
    id_libro INT NOT NULL,
    id_autor INT NOT NULL,
    PRIMARY KEY (id_libro, id_autor),
    CONSTRAINT fk_la_libro FOREIGN KEY (id_libro) REFERENCES libro(id_libro) ON DELETE CASCADE,
    CONSTRAINT fk_la_autor FOREIGN KEY (id_autor) REFERENCES autor(id_autor)
) ENGINE=InnoDB;

-- Copias fisicas de cada libro (un libro puede tener varios ejemplares)
CREATE TABLE ejemplar (
    id_ejemplar   INT AUTO_INCREMENT PRIMARY KEY,
    id_libro      INT NOT NULL,
    codigo        VARCHAR(30) NOT NULL UNIQUE,
    ubicacion     VARCHAR(60),
    estado        ENUM('DISPONIBLE','PRESTADO','RESERVADO','DANADO','PERDIDO') NOT NULL DEFAULT 'DISPONIBLE',
    fecha_ingreso DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_ejemplar_libro FOREIGN KEY (id_libro) REFERENCES libro(id_libro)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. PRESTAMOS, RESERVAS Y MULTAS
-- ---------------------------------------------------------------------
CREATE TABLE prestamo (
    id_prestamo      INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario       INT NOT NULL,                      -- quien recibe el prestamo
    id_bibliotecario INT NOT NULL,                      -- quien lo registra
    fecha_prestamo   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_limite     DATE NOT NULL,
    estado           ENUM('ACTIVO','FINALIZADO','VENCIDO') NOT NULL DEFAULT 'ACTIVO',
    observaciones    VARCHAR(255),
    renovaciones     INT NOT NULL DEFAULT 0,            -- cuantas veces se ha renovado
    CONSTRAINT fk_prestamo_usuario       FOREIGN KEY (id_usuario)       REFERENCES usuario(id_usuario),
    CONSTRAINT fk_prestamo_bibliotecario FOREIGN KEY (id_bibliotecario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE detalle_prestamo (
    id_detalle       INT AUTO_INCREMENT PRIMARY KEY,
    id_prestamo      INT NOT NULL,
    id_ejemplar      INT NOT NULL,
    fecha_devolucion DATETIME NULL,                     -- NULL = aun no devuelto
    estado_devolucion ENUM('BUEN_ESTADO','DANADO','PERDIDO') NULL,
    CONSTRAINT fk_dp_prestamo FOREIGN KEY (id_prestamo) REFERENCES prestamo(id_prestamo) ON DELETE CASCADE,
    CONSTRAINT fk_dp_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplar(id_ejemplar)
) ENGINE=InnoDB;

-- Cola de reservas: PENDIENTE = esperando turno; LISTA = ya hay un ejemplar apartado (id_ejemplar) que el usuario
-- debe recoger antes de fecha_limite_recogida; VENCIDA se guarda solo si una LISTA no se recogio a tiempo.
CREATE TABLE reserva (
    id_reserva            INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario            INT NOT NULL,
    id_libro              INT NOT NULL,
    fecha_reserva         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_vencimiento     DATE NOT NULL,
    estado                ENUM('PENDIENTE','LISTA','ATENDIDA','CANCELADA','VENCIDA') NOT NULL DEFAULT 'PENDIENTE',
    fecha_limite_recogida DATE NULL,
    id_ejemplar           INT NULL,
    CONSTRAINT fk_reserva_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuario(id_usuario),
    CONSTRAINT fk_reserva_libro    FOREIGN KEY (id_libro)    REFERENCES libro(id_libro),
    CONSTRAINT fk_reserva_ejemplar FOREIGN KEY (id_ejemplar) REFERENCES ejemplar(id_ejemplar)
) ENGINE=InnoDB;

CREATE TABLE multa (
    id_multa    INT AUTO_INCREMENT PRIMARY KEY,
    id_prestamo INT NOT NULL,
    id_usuario  INT NOT NULL,
    valor       DECIMAL(10,2) NOT NULL,
    motivo      ENUM('RETRASO','DANO','PERDIDA') NOT NULL,
    estado      ENUM('PENDIENTE','PAGADA') NOT NULL DEFAULT 'PENDIENTE',
    fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_pago  DATETIME NULL,
    CONSTRAINT fk_multa_prestamo FOREIGN KEY (id_prestamo) REFERENCES prestamo(id_prestamo),
    CONSTRAINT fk_multa_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Valores ajustables por el administrador (multas). Los cambia desde la pantalla de Multas.
CREATE TABLE configuracion (
    clave       VARCHAR(50)   NOT NULL PRIMARY KEY,
    valor       DECIMAL(10,2) NOT NULL,
    descripcion VARCHAR(150)  NOT NULL
) ENGINE=InnoDB;

-- Enlaces de recuperacion de contrasena (de un solo uso y con vencimiento)
CREATE TABLE recuperacion_clave (
    id_recuperacion INT AUTO_INCREMENT PRIMARY KEY,
    id_cuenta       INT      NOT NULL,
    token_hash      CHAR(64) NOT NULL,                    -- SHA-256 del token; el token en si nunca se guarda
    creada_en       DATETIME NOT NULL,
    expira_en       DATETIME NOT NULL,
    usada_en        DATETIME NULL,
    UNIQUE KEY uq_recuperacion_token (token_hash),
    INDEX idx_recuperacion_cuenta (id_cuenta),
    CONSTRAINT fk_recuperacion_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuenta(id_cuenta) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. AUDITORIA
-- ---------------------------------------------------------------------
CREATE TABLE auditoria (
    id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario   INT NULL,
    accion       VARCHAR(30) NOT NULL,                  -- LOGIN, LOGIN_FALLIDO, LOGOUT, CREAR, ACTUALIZAR, ELIMINAR...
    tabla        VARCHAR(50),
    id_registro  INT,
    detalle      VARCHAR(255),
    fecha        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    INDEX idx_auditoria_fecha (fecha)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. TRIGGERS: mantienen el estado del ejemplar al prestar / devolver
-- ---------------------------------------------------------------------
DELIMITER $$
CREATE TRIGGER trg_prestar_ejemplar AFTER INSERT ON detalle_prestamo
FOR EACH ROW
BEGIN
    UPDATE ejemplar SET estado = 'PRESTADO' WHERE id_ejemplar = NEW.id_ejemplar;
END$$

CREATE TRIGGER trg_devolver_ejemplar AFTER UPDATE ON detalle_prestamo
FOR EACH ROW
BEGIN
    IF OLD.fecha_devolucion IS NULL AND NEW.fecha_devolucion IS NOT NULL THEN
        UPDATE ejemplar
           SET estado = CASE NEW.estado_devolucion
                          WHEN 'DANADO'  THEN 'DANADO'
                          WHEN 'PERDIDO' THEN 'PERDIDO'
                          ELSE 'DISPONIBLE' END
         WHERE id_ejemplar = NEW.id_ejemplar;
    END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 6. VISTAS utiles para los modulos
-- ---------------------------------------------------------------------
CREATE VIEW v_libros_disponibilidad AS
SELECT l.id_libro, l.isbn, l.titulo, c.nombre AS categoria, e.nombre AS editorial,
       COUNT(x.id_ejemplar) AS total_ejemplares,
       SUM(x.estado = 'DISPONIBLE') AS disponibles
FROM libro l
JOIN categoria c ON c.id_categoria = l.id_categoria
JOIN editorial e ON e.id_editorial = l.id_editorial
LEFT JOIN ejemplar x ON x.id_libro = l.id_libro
GROUP BY l.id_libro, l.isbn, l.titulo, c.nombre, e.nombre;

CREATE VIEW v_prestamos_activos AS
SELECT p.id_prestamo, CONCAT(u.nombres,' ',u.apellidos) AS usuario, l.titulo,
       ex.codigo AS ejemplar, p.fecha_prestamo, p.fecha_limite,
       GREATEST(DATEDIFF(CURRENT_DATE, p.fecha_limite), 0) AS dias_retraso
FROM prestamo p
JOIN usuario u            ON u.id_usuario = p.id_usuario
JOIN detalle_prestamo d   ON d.id_prestamo = p.id_prestamo
JOIN ejemplar ex          ON ex.id_ejemplar = d.id_ejemplar
JOIN libro l              ON l.id_libro = ex.id_libro
WHERE d.fecha_devolucion IS NULL;

-- ---------------------------------------------------------------------
-- 7. DATOS INICIALES
-- Cuentas de prueba: admin, biblio (bibliotecario), carlos y laura (usuarios)  /  clave Admin123*
-- ---------------------------------------------------------------------
INSERT INTO rol (nombre, descripcion) VALUES
 ('ADMIN',        'Administra todo el sistema'),
 ('BIBLIOTECARIO','Gestiona libros y prestamos'),
 ('USUARIO',      'Consulta y solicita prestamos');

INSERT INTO usuario (documento, nombres, apellidos, email, telefono) VALUES
 ('1000000001','Administrador','Sistema','admin@biblosmast.com','3000000000'),
 ('1000000002','Beatriz','Rojas','biblio@biblosmast.com','3000000001'),
 ('1000000003','Carlos','Mejia','carlos@biblosmast.com','3000000002'),
 ('1000000004','Laura','Gomez','laura@biblosmast.com','3000000003');

-- Todas las cuentas de prueba usan la clave Admin123*
INSERT INTO cuenta (id_usuario, username, password_hash, id_rol) VALUES
 (1,'admin',  '$2a$10$nIrHXnZljgQngFyUUhpJpekfHxrNjBBOMiASF.2GKC0l/CTBeyyni',1),
 (2,'biblio', '$2a$10$nIrHXnZljgQngFyUUhpJpekfHxrNjBBOMiASF.2GKC0l/CTBeyyni',2),
 (3,'carlos', '$2a$10$nIrHXnZljgQngFyUUhpJpekfHxrNjBBOMiASF.2GKC0l/CTBeyyni',3),
 (4,'laura',  '$2a$10$nIrHXnZljgQngFyUUhpJpekfHxrNjBBOMiASF.2GKC0l/CTBeyyni',3);

INSERT INTO categoria (nombre, descripcion) VALUES
 ('Novela','Narrativa de ficcion'),
 ('Ciencia','Divulgacion y textos cientificos'),
 ('Tecnologia','Programacion e ingenieria'),
 ('Historia','Textos historicos');

INSERT INTO editorial (nombre, pais) VALUES
 ('Sudamericana','Argentina'),
 ('Alfaguara','España'),
 ('Prentice Hall','Estados Unidos');

INSERT INTO autor (nombres, apellidos, nacionalidad) VALUES
 ('Gabriel','García Márquez','Colombia'),
 ('Robert C.','Martin','Estados Unidos'),
 ('Carl','Sagan','Estados Unidos');

INSERT INTO libro (isbn, titulo, id_categoria, id_editorial, anio_publicacion, numero_paginas) VALUES
 ('9780307474728','Cien años de soledad',1,1,1967,471),
 ('9780132350884','Clean Code',3,3,2008,464),
 ('9780345539434','Cosmos',2,2,1980,396);

INSERT INTO libro_autor (id_libro, id_autor) VALUES (1,1),(2,2),(3,3);

INSERT INTO ejemplar (id_libro, codigo, ubicacion) VALUES
 (1,'LIB-0001-A','Estante A1'),(1,'LIB-0001-B','Estante A1'),
 (2,'LIB-0002-A','Estante T2'),
 (3,'LIB-0003-A','Estante C1');

-- Reservas de prueba: dos vigentes y una vencida (pendiente con la fecha ya pasada)
INSERT INTO reserva (id_usuario, id_libro, fecha_reserva, fecha_vencimiento) VALUES
 (3, 2, NOW(), DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY)),
 (4, 1, NOW(), DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY)),
 (3, 3, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY));

-- Valores de multas (el administrador los cambia desde la pantalla de Multas). Valores de ejemplo en pesos.
INSERT INTO configuracion (clave, valor, descripcion) VALUES
 ('multa_por_dia_retraso', 1000.00,  'Multa por cada día de retraso en la devolución (por ejemplar)'),
 ('multa_dano',            10000.00, 'Multa fija cuando un ejemplar se devuelve dañado'),
 ('multa_perdida',         30000.00, 'Multa fija cuando un ejemplar se pierde'),
 ('max_renovaciones',      2.00,     'Veces que se puede renovar un prestamo'),
 ('dias_prestamo',         7.00,     'Dias que se agregan a la fecha limite al renovar un prestamo'),
 ('dias_recogida_reserva', 3.00,     'Dias que tiene el usuario para recoger el ejemplar apartado por una reserva'),
 ('dias_vigencia_reserva', 14.00,    'Dias que vale una reserva que el usuario hace desde Mi cuenta');
