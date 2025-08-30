-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS participacion_db;

-- Usar la base de datos
USE participacion_db;

-- Crear la tabla participantes
CREATE TABLE IF NOT EXISTS participantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    apellidos_nombres VARCHAR(100) NOT NULL,
    numero_celular VARCHAR(20) NOT NULL,
    fecha_registro DATETIME NOT NULL,
    INDEX idx_numero_documento (numero_documento),
    INDEX idx_numero_celular (numero_celular)
);