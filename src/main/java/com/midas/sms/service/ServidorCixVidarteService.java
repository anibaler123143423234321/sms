package com.midas.sms.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.midas.sms.dto.ArchivoSistemaDTO;
import com.midas.sms.dto.AudioVentaDTO;
import com.midas.sms.dto.PaginaContenidoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServidorCixVidarteService {

    @Value("${asterisk.server.host}")
    private String remoteHost;

    @Value("${asterisk.server.user}")
    private String remoteUser;

    @Value("${asterisk.server.password}")
    private String remotePassword;

    @Value("${asterisk.server.port}")
    private int remotePort;

    // ❌ MÉTODO ANTIGUO (LENTO) - Mantener para compatibilidad con métodos existentes
    private static final String RUTA_BASE_MONITOR = "/var/spool/asterisk/monitorDONE";

    // ✅ NUEVO MÉTODO (RÁPIDO) - Usa script de búsqueda optimizado
    // Comandos: cd .. && cd BUSQUEDA && ./buscar_audios2 {numero}
    private static final String SCRIPT_BUSQUEDA = "./buscar_audios2";
    private static final String RUTA_RESULTADOS_BASE = "/BUSQUEDA/audios";
    private static final long TAMANO_MINIMO_BYTES = 180 * 1024; // 180 KB ≈ 3 minutos de audio

    public PaginaContenidoDTO listarContenidoPaginado(String subRuta, String terminoBusqueda, String fechaDesde,
            String fechaHasta, int pagina, int tamano) {
        String rutaCompleta = buildRutaCompleta(subRuta);

        Session session = null;
        Channel channel = null;
        ChannelSftp sftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(remoteUser, remoteHost, remotePort);
            session.setPassword(remotePassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setServerAliveInterval(15000);
            session.setServerAliveCountMax(2);

            try {
                session.connect(8000);
            } catch (JSchException ex) {
                String m = String.valueOf(ex.getMessage());
                if (m != null && m.toLowerCase(Locale.ROOT).contains("timeout")) {
                    throw new ResponseStatusException(
                            HttpStatus.GATEWAY_TIMEOUT,
                            "Timeout al conectar por SFTP al host destino");
                }
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Error SSH/SFTP: " + m);
            }

            channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;

            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(rutaCompleta);

            List<ArchivoSistemaDTO> todos = mapEntries(entries, terminoBusqueda, fechaDesde, fechaHasta);

            long total = todos.size();
            int size = Math.max(1, tamano);
            int totalPaginas = (int) Math.ceil((double) total / size);
            int paginaActual = Math.min(Math.max(1, pagina), Math.max(1, totalPaginas));
            int desde = (paginaActual - 1) * size;
            int hasta = (int) Math.min(desde + size, total);
            List<ArchivoSistemaDTO> page = (total > 0 && desde < hasta) ? todos.subList(desde, hasta)
                    : new ArrayList<>();

            return new PaginaContenidoDTO(paginaActual, totalPaginas, total, page);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al listar por SFTP");
        } finally {
            if (sftp != null && sftp.isConnected())
                sftp.disconnect();
            if (channel != null && channel.isConnected())
                channel.disconnect();
            if (session != null && session.isConnected())
                session.disconnect();
        }
    }

    private String buildRutaCompleta(String subRuta) {
        if (subRuta == null || subRuta.isBlank())
            return RUTA_BASE_MONITOR;

        String normalizada = subRuta.replace("\\", "/").trim();

        // ✅ Permitir rutas absolutas que empiezan con /BUSQUEDA
        if (normalizada.startsWith("/BUSQUEDA")) {
            return normalizada;
        }

        // ❌ Rechazar path traversal (..)
        if (normalizada.startsWith("..")) {
            throw new IllegalArgumentException("Ruta no válida.");
        }

        // ✅ Si empieza con /, es ruta absoluta válida
        if (normalizada.startsWith("/")) {
            return normalizada;
        }

        // Ruta relativa - agregar RUTA_BASE_MONITOR
        String ruta = RUTA_BASE_MONITOR + "/" + normalizada;
        // normalizar dobles barras
        return ruta.replace("//", "/");
    }

    private List<ArchivoSistemaDTO> mapEntries(Vector<ChannelSftp.LsEntry> entries, String terminoBusqueda,
            String fechaDesde, String fechaHasta) {
        List<ArchivoSistemaDTO> out = new ArrayList<>();
        SimpleDateFormat fechaFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        SimpleDateFormat horaFmt = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Parsear fechas de filtro si están presentes
        LocalDate fechaDesdeLocal = null;
        LocalDate fechaHastaLocal = null;

        if (fechaDesde != null && !fechaDesde.isBlank()) {
            try {
                fechaDesdeLocal = LocalDate.parse(fechaDesde, dateFormatter);
            } catch (Exception ex) {
                // Ignorar fecha inválida
            }
        }

        if (fechaHasta != null && !fechaHasta.isBlank()) {
            try {
                fechaHastaLocal = LocalDate.parse(fechaHasta, dateFormatter);
            } catch (Exception ex) {
                // Ignorar fecha inválida
            }
        }

        for (ChannelSftp.LsEntry e : entries) {
            String name = e.getFilename();
            if (".".equals(name) || "..".equals(name))
                continue;

            // Filtro por término de búsqueda
            if (terminoBusqueda != null && !terminoBusqueda.isBlank()
                    && !name.toLowerCase(Locale.ROOT).contains(terminoBusqueda.toLowerCase(Locale.ROOT))) {
                continue;
            }

            SftpATTRS a = e.getAttrs();
            boolean isDir = a.isDir();
            long size = a.getSize();
            long mtimeMillis = a.getMTime() * 1000L;
            Date mdate = new Date(mtimeMillis);

            String fechaArchivo = fechaFmt.format(mdate);

            // Filtro por rango de fechas
            if (fechaDesdeLocal != null || fechaHastaLocal != null) {
                try {
                    LocalDate fechaArchivoLocal = LocalDate.parse(fechaArchivo, dateFormatter);

                    if (fechaDesdeLocal != null && fechaArchivoLocal.isBefore(fechaDesdeLocal)) {
                        continue;
                    }

                    if (fechaHastaLocal != null && fechaArchivoLocal.isAfter(fechaHastaLocal)) {
                        continue;
                    }
                } catch (Exception ex) {
                    // Si no se puede parsear la fecha del archivo, incluirlo
                }
            }

            out.add(new ArchivoSistemaDTO(
                    name,
                    fechaArchivo,
                    horaFmt.format(mdate),
                    String.valueOf(size),
                    isDir));
        }

        // Ordenar: carpetas primero (ascendente), luego archivos (ascendente)
        out.sort(Comparator
                .comparing((ArchivoSistemaDTO archivo) -> !archivo.esDirectorio()) // Carpetas primero (false < true)
                .thenComparing(ArchivoSistemaDTO::nombre, String.CASE_INSENSITIVE_ORDER)); // Luego por nombre
                                                                                           // ascendente

        return out;
    }

    /**
     * Descarga un archivo individual del servidor remoto
     */
    public InputStream descargarArchivo(String ruta, String nombreArchivo) {
        String rutaCompleta = buildRutaCompleta(ruta);
        String rutaArchivo = rutaCompleta + "/" + nombreArchivo;

        Session session = null;
        Channel channel = null;
        ChannelSftp sftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(remoteUser, remoteHost, remotePort);
            session.setPassword(remotePassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setServerAliveInterval(15000);
            session.setServerAliveCountMax(2);

            session.connect(8000);
            channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;

            // Leer el archivo completo en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            sftp.get(rutaArchivo, baos);

            return new ByteArrayInputStream(baos.toByteArray());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado: " + nombreArchivo);
        } finally {
            if (sftp != null && sftp.isConnected())
                sftp.disconnect();
            if (channel != null && channel.isConnected())
                channel.disconnect();
            if (session != null && session.isConnected())
                session.disconnect();
        }
    }

    /**
     * Descarga múltiples archivos como un ZIP
     */
    public InputStream descargarArchivosComoZip(String ruta, List<String> nombresArchivos) {
        String rutaCompleta = buildRutaCompleta(ruta);

        Session session = null;
        Channel channel = null;
        ChannelSftp sftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(remoteUser, remoteHost, remotePort);
            session.setPassword(remotePassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setServerAliveInterval(15000);
            session.setServerAliveCountMax(2);

            session.connect(8000);
            channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            for (String nombreArchivo : nombresArchivos) {
                try {
                    String rutaArchivo = rutaCompleta + "/" + nombreArchivo;

                    // Crear entrada en el ZIP
                    ZipEntry zipEntry = new ZipEntry(nombreArchivo);
                    zipOut.putNextEntry(zipEntry);

                    // Descargar archivo y escribir al ZIP
                    ByteArrayOutputStream archivoStream = new ByteArrayOutputStream();
                    sftp.get(rutaArchivo, archivoStream);
                    zipOut.write(archivoStream.toByteArray());
                    zipOut.closeEntry();

                } catch (Exception e) {
                    // Continuar con el siguiente archivo si uno falla
                    System.err.println("Error descargando archivo: " + nombreArchivo + " - " + e.getMessage());
                }
            }

            zipOut.close();
            return new ByteArrayInputStream(baos.toByteArray());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creando ZIP: " + e.getMessage());
        } finally {
            if (sftp != null && sftp.isConnected())
                sftp.disconnect();
            if (channel != null && channel.isConnected())
                channel.disconnect();
            if (session != null && session.isConnected())
                session.disconnect();
        }
    }

    /**
     * ✅ NUEVO MÉTODO DE BÚSQUEDA RÁPIDA
     * Usa el script /BUSQUEDA/buscar_audios2 para búsqueda optimizada
     *
     * Flujo:
     * 1. Ejecuta: cd /BUSQUEDA && ./buscar_audios2 {numeroMovil}
     * 2. Script crea carpeta: /BUSQUEDA/audios/{fecha-actual}/{numeroMovil}/
     * 3. Lista archivos en esa carpeta
     * 4. Filtra archivos > 180 KB (≈ 3 minutos de duración)
     * 5. Ordena por fecha descendente
     * 6. Retorna resultados paginados
     *
     * @param numeroMovil Número de móvil a buscar
     * @param pagina Número de página (1-based)
     * @param tamano Tamaño de página
     * @return Página con los audios encontrados (filtrados por duración > 3 minutos)
     */
    public PaginaContenidoDTO buscarAudiosRapido(String numeroMovil, int pagina, int tamano) {
        Session session = null;
        ChannelExec channelExec = null;
        ChannelSftp sftpChannel = null;

        try {
            log.info("🔍 Búsqueda rápida para móvil: {}", numeroMovil);

            // 1. Conectar por SSH
            JSch jsch = new JSch();
            session = jsch.getSession(remoteUser, remoteHost, remotePort);
            session.setPassword(remotePassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setServerAliveInterval(15000);
            session.setServerAliveCountMax(2);
            session.connect(8000);

            // 2. Ejecutar script de búsqueda - PASO A PASO
            String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // ✅ Comandos paso a paso como indicaste:
            // 1. cd ..
            // 2. cd BUSQUEDA
            // 3. ./buscar_audios2 {numero}
            String comando = String.format("cd .. && cd BUSQUEDA && %s %s", SCRIPT_BUSQUEDA, numeroMovil);

            log.info("📡 Ejecutando comando: {}", comando);

            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(comando);
            channelExec.connect();

            // Esperar a que termine el script (máximo 30 segundos)
            int maxWait = 30;
            int waited = 0;
            while (!channelExec.isClosed() && waited < maxWait) {
                Thread.sleep(1000);
                waited++;
            }

            if (channelExec.getExitStatus() != 0) {
                log.warn("⚠️ Script retornó código: {}", channelExec.getExitStatus());
            }

            channelExec.disconnect();

            log.info("✅ Script ejecutado. Buscando archivos en: {}/{}/{}",
                RUTA_RESULTADOS_BASE, fechaHoy, numeroMovil);

            // 3. Listar archivos en la carpeta de resultados
            String rutaResultados = String.format("%s/%s/%s", RUTA_RESULTADOS_BASE, fechaHoy, numeroMovil);

            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;

            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(rutaResultados);

            // 4. Filtrar archivos (solo archivos, no directorios, y con tamaño > 1 minuto)
            List<AudioVentaDTO> archivos = new ArrayList<>();
            String fechaCreadaRuta = fechaHoy;

            for (ChannelSftp.LsEntry entry : entries) {
                String nombre = entry.getFilename();

                // Ignorar . y ..
                if (".".equals(nombre) || "..".equals(nombre)) {
                    continue;
                }

                SftpATTRS attrs = entry.getAttrs();
                boolean esDirectorio = attrs.isDir();

                // Solo archivos
                if (esDirectorio) {
                    continue;
                }

                long tamanoBytes = attrs.getSize();

                // Filtrar por tamaño (> 180 KB ≈ 3 minutos)
                if (tamanoBytes < TAMANO_MINIMO_BYTES) {
                    log.debug("⏩ Archivo {} ignorado (tamaño: {} bytes < {} bytes)",
                        nombre, tamanoBytes, TAMANO_MINIMO_BYTES);
                    continue;
                }

                // Formatear tamaño y calcular duración
                String tamanoFormateado = formatearTamano(tamanoBytes);
                String duracion = calcularDuracionAudio(tamanoBytes);

                // Extraer fecha y hora del timestamp
                long mtime = attrs.getMTime() * 1000L;
                Date fechaArchivo = new Date(mtime);
                SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss");

                AudioVentaDTO audio = AudioVentaDTO.builder()
                    .nombre(nombre)
                    .fechaCreadaRutaServidor(fechaCreadaRuta)
                    .hora(sdfHora.format(fechaArchivo))
                    .tamano(tamanoFormateado)
                    .tamanoBytes(String.valueOf(tamanoBytes))
                    .duracion(duracion)
                    .ipServidor(remoteHost)
                    .idLeadTranscrito(null)
                    .build();

                archivos.add(audio);
                log.debug("✅ Archivo encontrado: {} - {} - Duración: {} - IP: {}",
                    nombre, tamanoFormateado, duracion, remoteHost);
            }

            log.info("📊 Total archivos encontrados (> 3 min): {}", archivos.size());

            // 5. Ordenar por fecha/hora descendente (más recientes primero)
            archivos.sort((a, b) -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date fechaA = sdf.parse(a.getFechaCreadaRutaServidor() + " " + a.getHora());
                    Date fechaB = sdf.parse(b.getFechaCreadaRutaServidor() + " " + b.getHora());
                    return fechaB.compareTo(fechaA);
                } catch (ParseException e) {
                    return 0;
                }
            });

            // 6. Paginar resultados
            long total = archivos.size();
            int size = Math.max(1, tamano);
            int totalPaginas = (int) Math.ceil((double) total / size);
            int paginaActual = Math.min(Math.max(1, pagina), Math.max(1, totalPaginas));
            int desde = (paginaActual - 1) * size;
            int hasta = (int) Math.min(desde + size, total);
            List<AudioVentaDTO> page = (total > 0 && desde < hasta)
                ? archivos.subList(desde, hasta)
                : new ArrayList<>();

            log.info("📄 Página {}/{} - Mostrando {} de {} archivos",
                paginaActual, totalPaginas, page.size(), total);

            return new PaginaContenidoDTO(paginaActual, totalPaginas, total, page);

        } catch (Exception e) {
            log.error("❌ Error en búsqueda rápida: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error en búsqueda rápida: " + e.getMessage()
            );
        } finally {
            if (sftpChannel != null && sftpChannel.isConnected())
                sftpChannel.disconnect();
            if (channelExec != null && channelExec.isConnected())
                channelExec.disconnect();
            if (session != null && session.isConnected())
                session.disconnect();
        }
    }

    /**
     * Formatea el tamaño en bytes a formato legible (KB, MB)
     */
    private String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Calcula la duración aproximada del audio en formato mm:ss
     * Basado en: GSM codec = 13 kbps (1.625 KB/s)
     */
    private String calcularDuracionAudio(long bytes) {
        // GSM codec: ~1.625 KB por segundo (13 kbps / 8)
        double segundosTotales = bytes / 1625.0;
        int minutos = (int) (segundosTotales / 60);
        int segundos = (int) (segundosTotales % 60);
        return String.format("%d:%02d", minutos, segundos);
    }

    public void eliminarCarpetaAudios(String numeroMovil, String fecha) {
        Session session = null;
        ChannelExec channelExec = null;
        try {
            log.info("🗑️ Eliminando carpeta de audios para móvil: {} fecha: {}", numeroMovil, fecha);
            JSch jsch = new JSch();
            session = jsch.getSession(remoteUser, remoteHost, remotePort);
            session.setPassword(remotePassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setServerAliveInterval(15000);
            session.setServerAliveCountMax(2);
            session.connect(8000);
            String rutaCarpeta = String.format("%s/%s/%s", RUTA_RESULTADOS_BASE, fecha, numeroMovil);
            String comando = String.format("rm -rf %s", rutaCarpeta);
            log.info("📡 Ejecutando comando: {}", comando);
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(comando);
            channelExec.connect();
            int maxWait = 10, waited = 0;
            while (!channelExec.isClosed() && waited < maxWait) { Thread.sleep(1000); waited++; }
            int exitStatus = channelExec.getExitStatus();
            if (exitStatus == 0) log.info("✅ Carpeta eliminada exitosamente: {}", rutaCarpeta);
            else log.warn("⚠️ Comando retornó código: {} para ruta: {}", exitStatus, rutaCarpeta);
        } catch (Exception e) {
            log.error("❌ Error eliminando carpeta de audios: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error eliminando carpeta de audios: " + e.getMessage());
        } finally {
            if (channelExec != null && channelExec.isConnected()) channelExec.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }
}
