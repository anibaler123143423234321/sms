package com.midas.sms.service;

import com.midas.sms.dto.ArchivoSistemaDTO;
import com.midas.sms.dto.AudioVentaDTO;
import com.midas.sms.dto.BuscarAudiosVentaRequest;
import com.midas.sms.dto.BuscarAudiosVentaResponse;
import com.midas.sms.dto.PaginaContenidoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaAudioService {

    // Servicios de CHICLAYO únicamente
    private final ServidorCixVidarteService servidorCixVidarteService;
    private final ServidorCixTantaleanService servidorCixTantaleanService;
    private final ServidorCixMiguelKevinService servidorCixMiguelKevinService;
    private final ServidorCixJulcaService servidorCixJulcaService;
    private final ServidorCixSolivesa2Service servidorCixSolivesa2Service;

    @Value("${sms.service.base.url:https://apisozarusac.com/BackendArchivos}")
    private String baseUrl;

    // Mapeo de número de servidor a endpoint (SOLO CHICLAYO)
    private static final Map<String, String> SERVIDOR_A_ENDPOINT = new HashMap<>();

    static {
        // ✅ Mapeo correcto según application.properties
        SERVIDOR_A_ENDPOINT.put("23", "/api/monitor-cix-tantalean");     // 104.128.75.23 - MARYORITH TANTALEAN
        SERVIDOR_A_ENDPOINT.put("31", "/api/monitor-cix-kevin");         // 172.96.143.31 - KEVIN QUISPE
        SERVIDOR_A_ENDPOINT.put("126", "/api/monitor-cix-julca");        // 45.92.18.126 - JOSSEY JULCA
        SERVIDOR_A_ENDPOINT.put("154", "/api/monitor-cix-vidarte");      // 104.243.47.154 - VIDARTE/JUAPE
        SERVIDOR_A_ENDPOINT.put("157", "/api/monitor-cix-solivesa2");    // 104.243.47.157 - SOLIVESA 2
    }

    /**
     * Busca los audios de una venta en el servidor de archivos
     * Si no encuentra en el servidor especificado, busca en los servidores SOLIVESA
     */
    public BuscarAudiosVentaResponse buscarAudiosVenta(BuscarAudiosVentaRequest request) {
        log.info("🔍 Buscando audios para venta - Servidor: {}, Fecha: {}, Móvil: {}",
            request.getNumeroServidor(), request.getFechaRegistro(), request.getMovilContacto());

        try {
            // 1. Validar datos de entrada
            if (request.getNumeroServidor() == null || request.getFechaRegistro() == null || request.getMovilContacto() == null) {
                return BuscarAudiosVentaResponse.builder()
                    .success(false)
                    .message("Faltan datos requeridos: numeroServidor, fechaRegistro o movilContacto")
                    .audios(new ArrayList<>())
                    .totalAudios(0)
                    .build();
            }

            // 2. Buscar en el servidor principal
            List<AudioVentaDTO> audios = buscarEnServidor(request);

            // 3. Si no se encontraron audios, buscar en los servidores SOLIVESA
            if (audios.isEmpty()) {
                log.info("⚠️ No se encontraron audios en servidor {}. Buscando en servidores SOLIVESA...", request.getNumeroServidor());

                // Buscar en SOLIVESA 1 (servidor 14)
                BuscarAudiosVentaRequest requestSolivesa1 = BuscarAudiosVentaRequest.builder()
                    .numeroServidor("14")
                    .fechaRegistro(request.getFechaRegistro())
                    .movilContacto(request.getMovilContacto())
                    .numeroAgente(request.getNumeroAgente())
                    .build();
                List<AudioVentaDTO> audiosSolivesa1 = buscarEnServidor(requestSolivesa1);
                audios.addAll(audiosSolivesa1);

                // Buscar en SOLIVESA 2 (servidor 157)
                BuscarAudiosVentaRequest requestSolivesa2 = BuscarAudiosVentaRequest.builder()
                    .numeroServidor("157")
                    .fechaRegistro(request.getFechaRegistro())
                    .movilContacto(request.getMovilContacto())
                    .numeroAgente(request.getNumeroAgente())
                    .build();
                List<AudioVentaDTO> audiosSolivesa2 = buscarEnServidor(requestSolivesa2);
                audios.addAll(audiosSolivesa2);

                if (!audios.isEmpty()) {
                    log.info("✅ Se encontraron {} audios en servidores SOLIVESA", audios.size());
                }
            }

            // 4. Eliminar duplicados finales
            audios = audios.stream()
                .distinct()
                .collect(Collectors.toList());

            String mensaje = audios.isEmpty()
                ? "No se encontraron audios en ningún servidor"
                : "Búsqueda completada exitosamente";

            return BuscarAudiosVentaResponse.builder()
                .success(true)
                .message(mensaje)
                .audios(audios)
                .totalAudios(audios.size())
                .build();

        } catch (Exception e) {
            log.error("❌ Error buscando audios: {}", e.getMessage(), e);
            return BuscarAudiosVentaResponse.builder()
                .success(false)
                .message("Error al buscar audios: " + e.getMessage())
                .audios(new ArrayList<>())
                .totalAudios(0)
                .build();
        }
    }

    /**
     * Busca audios en un servidor específico (MÉTODO ORIGINAL)
     */
    private List<AudioVentaDTO> buscarEnServidor(BuscarAudiosVentaRequest request) {
        List<AudioVentaDTO> audios = new ArrayList<>();

        try {
            // 1. Obtener el endpoint del servidor
            String endpoint = SERVIDOR_A_ENDPOINT.get(request.getNumeroServidor());
            if (endpoint == null) {
                log.warn("❌ No se encontró endpoint para servidor: {}", request.getNumeroServidor());
                return audios;
            }

            // 2. Convertir fecha ISO a formato DDMMYYYY
            String fechaFormateada = convertirFechaAFormatoArchivo(request.getFechaRegistro());

            // 3. Extraer todos los números de móvil del campo movilContacto
            List<String> moviles = extraerNumerosMoviles(request.getMovilContacto());

            // 4. Buscar en ambas rutas: celulares y fijos, para cada móvil
            for (String movil : moviles) {
                audios.addAll(buscarEnRuta(request.getNumeroServidor(), "GSM/spain/celulares/" + fechaFormateada, movil, endpoint));
                audios.addAll(buscarEnRuta(request.getNumeroServidor(), "GSM/spain/fijos/" + fechaFormateada, movil, endpoint));

                // También buscar sin el primer dígito (por si el archivo tiene formato diferente)
                if (movil.length() > 8) {
                    String movilSinPrefijo = movil.substring(1);
                    audios.addAll(buscarEnRuta(request.getNumeroServidor(), "GSM/spain/celulares/" + fechaFormateada, movilSinPrefijo, endpoint));
                    audios.addAll(buscarEnRuta(request.getNumeroServidor(), "GSM/spain/fijos/" + fechaFormateada, movilSinPrefijo, endpoint));
                }
            }

            // 5. Eliminar duplicados
            audios = audios.stream()
                .distinct()
                .collect(Collectors.toList());

            // 6. Filtrar por número de agente si existe
            // TODO: Implementar filtro por agente si es necesario en el futuro
            // Por ahora el filtro se hace en el frontend

        } catch (Exception e) {
            log.warn("⚠️ Error buscando en servidor {}: {}", request.getNumeroServidor(), e.getMessage());
        }

        return audios;
    }

    /**
     * Busca archivos en una ruta específica del servidor
     */
    private List<AudioVentaDTO> buscarEnRuta(String numeroServidor, String ruta, String movilContacto, String endpoint) {
        List<AudioVentaDTO> audios = new ArrayList<>();

        try {
            log.info("📁 Buscando en ruta: {}", ruta);

            // Obtener el servicio correspondiente según el número de servidor
            PaginaContenidoDTO paginaContenido = obtenerContenidoServidor(numeroServidor, ruta);

            if (paginaContenido == null || paginaContenido.contenido() == null) {
                log.warn("⚠️ No se encontró contenido en ruta: {}", ruta);
                return audios;
            }

            // Filtrar archivos que contengan el móvil de contacto
            for (Object obj : paginaContenido.contenido()) {
                if (obj instanceof ArchivoSistemaDTO archivo) {
                    if (!archivo.esDirectorio() && archivo.nombre().contains(movilContacto)) {
                        AudioVentaDTO audio = AudioVentaDTO.builder()
                            .nombre(archivo.nombre())
                            .fechaCreadaRutaServidor(archivo.fecha())
                            .hora(archivo.hora())
                            .tamano(archivo.tamano())
                            .ipServidor(null) // No disponible en método antiguo
                            .build();

                        audios.add(audio);
                        log.info("✅ Audio encontrado: {}", archivo.nombre());
                    }
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ Error buscando en ruta {}: {}", ruta, e.getMessage());
        }

        return audios;
    }

    /**
     * Obtiene el contenido del servidor según el número de servidor (SOLO CHICLAYO)
     */
    private PaginaContenidoDTO obtenerContenidoServidor(String numeroServidor, String ruta) {
        try {
            return switch (numeroServidor) {
                case "154" -> servidorCixVidarteService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "23" -> servidorCixTantaleanService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "31" -> servidorCixMiguelKevinService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "126" -> servidorCixJulcaService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "157" -> servidorCixSolivesa2Service.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                default -> {
                    log.warn("⚠️ Servidor no encontrado: {}", numeroServidor);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("❌ Error obteniendo contenido del servidor {}: {}", numeroServidor, e.getMessage());
            return null;
        }
    }

    /**
     * Convierte fecha ISO a formato DDMMYYYY
     * Ejemplo: "2025-10-15T21:54:06" -> "15102025"
     */
    private String convertirFechaAFormatoArchivo(String fechaISO) {
        try {
            LocalDateTime fecha = LocalDateTime.parse(fechaISO, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return fecha.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        } catch (Exception e) {
            // Si falla, intentar solo con la fecha
            try {
                String[] partes = fechaISO.split("T");
                String[] fechaPartes = partes[0].split("-");
                return fechaPartes[2] + fechaPartes[1] + fechaPartes[0];
            } catch (Exception ex) {
                log.error("❌ Error convirtiendo fecha: {}", fechaISO);
                return "";
            }
        }
    }

    /**
     * Extrae todos los números de móvil del campo movilContacto
     * Ejemplo: "624784798 ( 960432023 SOLIVEA )" → ["624784798", "960432023"]
     */
    private List<String> extraerNumerosMoviles(String movilContacto) {
        List<String> moviles = new ArrayList<>();

        if (movilContacto == null || movilContacto.isEmpty()) {
            return moviles;
        }

        // Extraer todos los números de 9 dígitos o más
        Pattern pattern = Pattern.compile("\\d{9,}");
        Matcher matcher = pattern.matcher(movilContacto);

        while (matcher.find()) {
            String numero = matcher.group();
            // Limpiar prefijos internacionales
            numero = numero.replaceAll("^(34|0034)", "");
            // Quitar ceros iniciales pero mantener al menos 9 dígitos
            while (numero.length() > 9 && numero.startsWith("0")) {
                numero = numero.substring(1);
            }
            if (numero.length() >= 9) {
                moviles.add(numero);
            }
        }

        // Si no se encontró ningún número, intentar limpiar el string completo
        if (moviles.isEmpty()) {
            String limpio = movilContacto.replaceAll("[^0-9]", "");
            if (limpio.length() >= 9) {
                moviles.add(limpio);
            }
        }

        return moviles;
    }

    /**
     * Extrae el número de agente del nombre del archivo
     * Soporta 2 formatos:
     * - Formato 1: {extension}{movilContacto}-{numeroAgente}-{DD-MM-YYYY-HH-MM-SS}.gsm
     *   Ejemplo: "022626047261-8009-16-10-2025-14-42-47.gsm" → "8009"
     * - Formato 2: {YYYYMMDD}-{HHMMSS}_{movil}_{campaign}_agent{agente}-{suffix}.gsm
     *   Ejemplo: "20251018-182407_609229717_TESTCAMP_agent012-all.gsm" → "012"
     */
    private String extraerNumeroAgente(String nombreArchivo, String movilContacto) {
        // Intentar Formato 2: agent{numero}
        Pattern patternAgent = Pattern.compile("agent(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcherAgent = patternAgent.matcher(nombreArchivo);
        if (matcherAgent.find()) {
            return matcherAgent.group(1);
        }

        // Intentar Formato 1: {extension}{movil}-{agente}-{fecha}.gsm
        Pattern pattern = Pattern.compile("\\d+-(\\d+)-\\d{2}-\\d{2}-\\d{4}");
        Matcher matcher = pattern.matcher(nombreArchivo);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Extrae la extensión del nombre del archivo
     * Soporta 2 formatos:
     * - Formato 1: {extension}{movilContacto}-{numeroAgente}-{DD-MM-YYYY-HH-MM-SS}.gsm
     *   Ejemplo: "022626047261-8009-16-10-2025-14-42-47.gsm" con móvil "626047261" → "022"
     * - Formato 2: {YYYYMMDD}-{HHMMSS}_{movil}_{campaign}_agent{agente}-{suffix}.gsm
     *   Ejemplo: "20251018-182407_609229717_TESTCAMP_agent012-all.gsm" → "012" (mismo que agente)
     */
    private String extraerExtension(String nombreArchivo, String movilContacto) {
        // Intentar Formato 2: usar el número de agente como extensión
        Pattern patternAgent = Pattern.compile("agent(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcherAgent = patternAgent.matcher(nombreArchivo);
        if (matcherAgent.find()) {
            return matcherAgent.group(1);
        }

        // Intentar Formato 1: extraer prefijo antes del móvil
        int indexMovil = nombreArchivo.indexOf(movilContacto);
        if (indexMovil > 0) {
            return nombreArchivo.substring(0, indexMovil);
        }

        return null;
    }
}

