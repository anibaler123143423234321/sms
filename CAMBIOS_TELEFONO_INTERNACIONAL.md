# Cambios para Soporte de Teléfonos Internacionales

## Resumen
Se han actualizado los siguientes archivos para soportar números de teléfono con prefijos internacionales (+34, +1, etc.) de hasta 20 caracteres:

## Archivos Modificados

### 1. Backend - DTO
**Archivo:** `src/main/java/com/midas/sms/dto/ClienteDTO.java`
- **Cambio:** Actualizada la validación del campo `numeroCelular`
- **Antes:** `@Pattern(regexp = "^[0-9]{9}$", message = "El número de celular debe tener 9 dígitos")`
- **Después:** `@Pattern(regexp = "^\\+[0-9]{1,4}[0-9]{9,15}$", message = "El número de celular debe incluir prefijo internacional (+34, +1, etc.) y tener entre 9 y 15 dígitos")`
- **Agregado:** `@Size(max = 20, message = "El número de celular no puede exceder 20 caracteres")`

### 2. Backend - Entidad
**Archivo:** `src/main/java/com/midas/sms/entity/Cliente.java`
- **Cambio:** Aumentado el tamaño del campo `numero_celular`
- **Antes:** `@Column(name = "numero_celular", nullable = false, length = 15)`
- **Después:** `@Column(name = "numero_celular", nullable = false, length = 20)`

### 3. Base de Datos - Schema
**Archivo:** `src/main/resources/schema.sql`
- **Cambio:** Actualizado el tamaño del campo en la tabla
- **Antes:** `numero_celular VARCHAR(15) NOT NULL,`
- **Después:** `numero_celular VARCHAR(20) NOT NULL,`

### 4. Migración de Base de Datos
**Archivo:** `migration_update_phone_field.sql` (NUEVO)
- Script para actualizar bases de datos existentes
- Modifica el campo `numero_celular` de VARCHAR(15) a VARCHAR(20)

## Validaciones Actualizadas

### Patrón de Validación
- **Formato esperado:** `+[código país][número teléfono]`
- **Ejemplos válidos:**
  - `+34123456789` (España)
  - `+1234567890` (Estados Unidos)
  - `+33123456789` (Francia)
  - `+49123456789` (Alemania)

### Longitud
- **Mínimo:** 12 caracteres (prefijo + 9 dígitos)
- **Máximo:** 20 caracteres (prefijo + hasta 15 dígitos)

## Instrucciones de Despliegue

1. **Ejecutar migración de base de datos:**
   ```sql
   -- Ejecutar el archivo migration_update_phone_field.sql
   ALTER TABLE clientes MODIFY COLUMN numero_celular VARCHAR(20) NOT NULL;
   ```

2. **Reiniciar la aplicación Spring Boot** para que tome los nuevos cambios en las validaciones.

3. **Verificar funcionamiento** enviando una petición POST con un número internacional:
   ```json
   {
     "tipoDocumento": "DNI",
     "numeroDocumento": "12345678",
     "apellidosNombres": "Juan Pérez",
     "numeroCelular": "+34123456789"
   }
   ```

## Notas Importantes
- Los números existentes en la base de datos que no tengan prefijo seguirán funcionando
- Se recomienda migrar gradualmente los números existentes para incluir el prefijo +34
- El frontend ya está configurado para enviar números con prefijo internacional