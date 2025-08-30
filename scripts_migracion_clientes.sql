-- =====================================================
-- MIGRACIÓN: PARTICIPANTES → CLIENTES + TRACKING SMS
-- =====================================================

-- 1. RENOMBRAR TABLA PRINCIPAL
ALTER TABLE participantes RENAME TO clientes;

-- 2. AGREGAR NUEVOS CAMPOS A SMS_MESSAGES PARA TRACKING
ALTER TABLE sms_messages ADD COLUMN campania VARCHAR(100);
ALTER TABLE sms_messages ADD COLUMN tipo_sms VARCHAR(100);
ALTER TABLE sms_messages ADD COLUMN nombre_cliente VARCHAR(200);
ALTER TABLE sms_messages ADD COLUMN codigo_tracking VARCHAR(50) UNIQUE;
ALTER TABLE sms_messages ADD COLUMN url_completa VARCHAR(500);

-- 3. CREAR TABLA DE TRACKING SMS
CREATE TABLE sms_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_tracking VARCHAR(50) UNIQUE NOT NULL,
    sms_message_id BIGINT,
    user_id BIGINT,
    nombre_cliente VARCHAR(200),
    numero_telefono VARCHAR(20),
    fecha_envio DATETIME,
    fecha_visita DATETIME,
    ip_visita VARCHAR(45),
    visitado BOOLEAN DEFAULT FALSE,
    user_agent TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (sms_message_id) REFERENCES sms_messages(id) ON DELETE CASCADE,
    INDEX idx_codigo_tracking (codigo_tracking),
    INDEX idx_user_id (user_id),
    INDEX idx_fecha_envio (fecha_envio),
    INDEX idx_visitado (visitado)
);

-- 4. CREAR ÍNDICES PARA OPTIMIZACIÓN
CREATE INDEX idx_sms_codigo_tracking ON sms_messages(codigo_tracking);
CREATE INDEX idx_sms_campania ON sms_messages(campania);
CREATE INDEX idx_sms_tipo_sms ON sms_messages(tipo_sms);
CREATE INDEX idx_clientes_numero_celular ON clientes(numero_celular);
CREATE INDEX idx_clientes_numero_documento ON clientes(numero_documento);

-- 5. VERIFICAR CAMBIOS
SELECT 'Migración completada exitosamente' as resultado;

-- Verificar tabla clientes
SELECT COUNT(*) as total_clientes FROM clientes;

-- Verificar nuevos campos en sms_messages
DESCRIBE sms_messages;

-- Verificar tabla de tracking
DESCRIBE sms_tracking;