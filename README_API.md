# API de Participantes - Documentación

## Configuración de Base de Datos

1. Instalar MySQL
2. Crear usuario y base de datos:
```sql
CREATE DATABASE participacion_db;
CREATE USER 'participacion_user'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON participacion_db.* TO 'participacion_user'@'localhost';
FLUSH PRIVILEGES;
```

3. Actualizar `application.properties` con tus credenciales de MySQL

## Endpoints de la API

### Base URL: `http://localhost:8080/api/participantes`

### 1. Registrar Participante
**POST** `/registrar`

**Body (JSON):**
```json
{
    "tipoDocumento": "DNI",
    "numeroDocumento": "12345678",
    "apellidosNombres": "García López, Juan Carlos",
    "numeroCelular": "987654321"
}
```

**Respuesta exitosa:**
```json
{
    "success": true,
    "message": "Participante registrado exitosamente",
    "data": {
        "id": 1,
        "tipoDocumento": "DNI",
        "numeroDocumento": "12345678",
        "apellidosNombres": "García López, Juan Carlos",
        "numeroCelular": "987654321",
        "fechaRegistro": "2025-01-08T10:30:00"
    }
}
```

### 2. Listar Participantes
**GET** `/listar`

**Respuesta:**
```json
{
    "success": true,
    "message": "Participantes obtenidos exitosamente",
    "data": [...]
}
```

### 3. Buscar Participante por Documento
**GET** `/buscar/{numeroDocumento}`

### 4. Contar Participantes
**GET** `/contar`

## Validaciones

- **Tipo de documento**: Obligatorio
- **Número de documento**: Obligatorio, único, 8-20 caracteres
- **Apellidos y nombres**: Obligatorio, 2-100 caracteres
- **Número de celular**: Obligatorio, 9 dígitos, único

## Códigos de Error

- `400 Bad Request`: Errores de validación o datos duplicados
- `404 Not Found`: Participante no encontrado
- `500 Internal Server Error`: Error del servidor

## Ejemplo de uso con curl

```bash
# Registrar participante
curl -X POST http://localhost:8080/api/participantes/registrar \
  -H "Content-Type: application/json" \
  -d '{
    "tipoDocumento": "DNI",
    "numeroDocumento": "12345678",
    "apellidosNombres": "García López, Juan Carlos",
    "numeroCelular": "987654321"
  }'

# Listar participantes
curl http://localhost:8080/api/participantes/listar

# Contar participantes
curl http://localhost:8080/api/participantes/contar
```