-- =====================================================
-- MIGRACIÓN: ACTUALIZAR CAMPO NUMERO_CELULAR PARA SOPORTAR PREFIJOS INTERNACIONALES
-- =====================================================

-- Actualizar el tamaño del campo numero_celular en la tabla clientes
-- De VARCHAR(15) a VARCHAR(20) para soportar prefijos internacionales como +34
ALTER TABLE clientes MODIFY COLUMN numero_celular VARCHAR(20) NOT NULL;

-- Verificar el cambio
DESCRIBE clientes;

-- Mostrar algunos registros para verificar
SELECT id, numero_celular FROM clientes LIMIT 5;

SELECT 'Migración de campo numero_celular completada exitosamente' as resultado;