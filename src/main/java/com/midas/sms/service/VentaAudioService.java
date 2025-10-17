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

    private final ServidorCixMaryoryJoseyService servidorCixMaryoryJoseyService;
    private final ServidorCixVidarteService servidorCixVidarteService;
    private final ServidorCixMiguelKevinService servidorCixMiguelKevinService;
    private final ServidorSanMiguelMayraAngeloService servidorSanMiguelMayraAngeloService;
    private final ServidorCixRaynerEstradaService servidorCixRaynerEstradaService;
    private final ServidorLosOlivosRonierYuvvisiaAlinsonService servidorLosOlivosRonierYuvvisiaAlinsonService;
    private final ServidorLosOlivosAscencioService servidorLosOlivosAscencioService;
    private final ServidorSanMiguelMariaJoseService servidorSanMiguelMariaJoseService;

    @Value("${sms.service.base.url:https://apisozarusac.com/BackendArchivos}")
    private String baseUrl;

    // Mapeo de número de servidor a endpoint
    private static final Map<String, String> SERVIDOR_A_ENDPOINT = new HashMap<>();
    
    static {
        SERVIDOR_A_ENDPOINT.put("154", "/api/monitordone");
        SERVIDOR_A_ENDPOINT.put("23", "/api/monitor-cix-maryory-josey");
        SERVIDOR_A_ENDPOINT.put("31", "/api/monitor-cix-miguel-kevin");
        SERVIDOR_A_ENDPOINT.put("32", "/api/monitor-san-miguel-mayra-angelo");
        SERVIDOR_A_ENDPOINT.put("33", "/api/monitor-cix-rayner-estrada");
        SERVIDOR_A_ENDPOINT.put("34", "/api/monitor-los-olivos-ronier-yuvvisia-alinson");
        SERVIDOR_A_ENDPOINT.put("35", "/api/monitor-los-olivos-ascencio");
        SERVIDOR_A_ENDPOINT.put("36", "/api/monitor-san-miguel-maria-jose");
    }

    /**
     * Busca los audios de una venta en el servidor de archivos
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

            // 2. Obtener el endpoint del servidor
            String endpoint = SERVIDOR_A_ENDPOINT.get(request.getNumeroServidor());
            if (endpoint == null) {
                log.warn("❌ No se encontró endpoint para servidor: {}", request.getNumeroServidor());
                return BuscarAudiosVentaResponse.builder()
                    .success(false)
                    .message("Servidor no encontrado: " + request.getNumeroServidor())
                    .audios(new ArrayList<>())
                    .totalAudios(0)
                    .build();
            }

            // 3. Convertir fecha ISO a formato DDMMYYYY
            String fechaFormateada = convertirFechaAFormatoArchivo(request.getFechaRegistro());
            
            // 4. Limpiar móvil de contacto
            String movilLimpio = limpiarMovilContacto(request.getMovilContacto());

            log.info("📅 Fecha formateada: {}, Móvil limpio: {}, Endpoint: {}", 
                fechaFormateada, movilLimpio, endpoint);

            // 5. Buscar en ambas rutas: celulares y fijos
            List<AudioVentaDTO> audios = new ArrayList<>();
            audios.addAll(buscarEnRuta(request.getNumeroServidor(), "GSM/spain/celulares/" + fechaFormateada, movilLimpio, endpoint));
            audios.addAll(buscarEnRuta(request.getNumeroServidor(), "GSM/spain/fijos/" + fechaFormateada, movilLimpio, endpoint));

            // 6. Filtrar por número de agente si existe
            if (request.getNumeroAgente() != null && !request.getNumeroAgente().isEmpty()) {
                String agenteOriginal = request.getNumeroAgente();
                log.info("🔍 Filtrando por agente: {}", agenteOriginal);

                audios = audios.stream()
                    .filter(audio -> {
                        if (audio.getNumeroAgente() == null) return false;

                        // Comparar sin ceros a la izquierda: "009" == "9", "022" == "22"
                        String agenteAudio = audio.getNumeroAgente().replaceFirst("^0+(?!$)", "");
                        String agenteRequest = agenteOriginal.replaceFirst("^0+(?!$)", "");

                        boolean match = agenteAudio.equals(agenteRequest);
                        log.debug("Comparando agente audio '{}' con request '{}': {}",
                            audio.getNumeroAgente(), agenteOriginal, match);
                        return match;
                    })
                    .collect(Collectors.toList());

                log.info("✅ Audios después de filtrar por agente: {}", audios.size());
            }

            log.info("✅ Total audios encontrados: {}", audios.size());

            return BuscarAudiosVentaResponse.builder()
                .success(true)
                .message("Búsqueda completada exitosamente")
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
            for (ArchivoSistemaDTO archivo : paginaContenido.contenido()) {
                if (!archivo.esDirectorio() && archivo.nombre().contains(movilContacto)) {
                    // Construir URL completa
                    String urlCompleta = String.format("%s%s/archivos/descargar?ruta=%s/%s",
                        baseUrl, endpoint, ruta, archivo.nombre());

                    // Extraer número de agente y extensión del nombre del archivo
                    String numeroAgente = extraerNumeroAgente(archivo.nombre(), movilContacto);
                    String extension = extraerExtension(archivo.nombre());

                    AudioVentaDTO audio = AudioVentaDTO.builder()
                        .nombre(archivo.nombre())
                        .fecha(archivo.fecha())
                        .hora(archivo.hora())
                        .tamano(archivo.tamano())
                        .urlCompleta(urlCompleta)
                        .numeroAgente(numeroAgente)
                        .extension(extension)
                        .build();

                    audios.add(audio);
                    log.info("✅ Audio encontrado: {} - Agente: {}", archivo.nombre(), numeroAgente);
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ Error buscando en ruta {}: {}", ruta, e.getMessage());
        }

        return audios;
    }

    /**
     * Obtiene el contenido del servidor según el número de servidor
     */
    private PaginaContenidoDTO obtenerContenidoServidor(String numeroServidor, String ruta) {
        try {
            return switch (numeroServidor) {
                case "23" -> servidorCixMaryoryJoseyService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "154" -> servidorCixVidarteService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "31" -> servidorCixMiguelKevinService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "32" -> servidorSanMiguelMayraAngeloService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "33" -> servidorCixRaynerEstradaService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "34" -> servidorLosOlivosRonierYuvvisiaAlinsonService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "35" -> servidorLosOlivosAscencioService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                case "36" -> servidorSanMiguelMariaJoseService.listarContenidoPaginado(ruta, null, null, null, 1, 500);
                default -> null;
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
     * Limpia el móvil de contacto quitando prefijos internacionales
     */
    private String limpiarMovilContacto(String movil) {
        String limpio = movil.replaceAll("[\\s\\-\\(\\)]", "");
        limpio = limpio.replaceAll("^(\\+34|0034)", "");
        limpio = limpio.replaceAll("^0+", "");
        return limpio;
    }

    /**
     * Extrae el número de agente del nombre del archivo
     * Ejemplo: "022606358444-8007-16-10-2025-13-49-28.gsm" con móvil "606358444" → "022"
     */
    private String extraerNumeroAgente(String nombreArchivo, String movilContacto) {
        int indexMovil = nombreArchivo.indexOf(movilContacto);
        if (indexMovil > 0) {
            return nombreArchivo.substring(0, indexMovil);
        }
        return null;
    }

    /**
     * Extrae la extensión del nombre del archivo
     * Ejemplo: "022606358444-8007-16-10-2025-13-49-28.gsm" → "8007"
     */
    private String extraerExtension(String nombreArchivo) {
        // Patrón: {agente}{movil}-{extension}-{fecha}.gsm
        Pattern pattern = Pattern.compile("\\d+-(\\d+)-\\d{2}-\\d{2}-\\d{4}");
        Matcher matcher = pattern.matcher(nombreArchivo);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

