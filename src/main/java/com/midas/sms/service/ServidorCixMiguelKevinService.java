package com.midas.sms.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.midas.sms.dto.ArchivoSistemaDTO;
import com.midas.sms.dto.PaginaContenidoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

@Service
@RequiredArgsConstructor
public class ServidorCixMiguelKevinService {

    @Value("${asterisk.cix.miguel.kevin.server.host}")
    private String remoteHost;

    @Value("${asterisk.cix.miguel.kevin.server.user}")
    private String remoteUser;

    @Value("${asterisk.cix.miguel.kevin.server.password}")
    private String remotePassword;

    @Value("${asterisk.cix.miguel.kevin.server.port}")
    private int remotePort;

    private static final String RUTA_BASE_MONITOR = "/var/spool/asterisk/monitorDONE";

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
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                            "Timeout al conectar por SFTP al host destino");
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error SSH/SFTP: " + m);
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
        if (normalizada.startsWith("..") || normalizada.startsWith("/")) {
            throw new IllegalArgumentException("Ruta no válida.");
        }
        String ruta = RUTA_BASE_MONITOR + "/" + normalizada;
        return ruta.replace("//", "/");
    }

    private List<ArchivoSistemaDTO> mapEntries(Vector<ChannelSftp.LsEntry> entries, String terminoBusqueda,
            String fechaDesde, String fechaHasta) {
        List<ArchivoSistemaDTO> out = new ArrayList<>();
        SimpleDateFormat fechaFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        SimpleDateFormat horaFmt = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

            out.add(new ArchivoSistemaDTO(name, fechaArchivo, horaFmt.format(mdate), String.valueOf(size), isDir));
        }

        // Ordenar: carpetas primero (ascendente), luego archivos (ascendente)
        out.sort(Comparator
                .comparing((ArchivoSistemaDTO archivo) -> !archivo.esDirectorio()) // Carpetas primero (false < true)
                .thenComparing(ArchivoSistemaDTO::nombre, String.CASE_INSENSITIVE_ORDER)); // Luego por nombre
                                                                                           // ascendente

        return out;
    }

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

                    ZipEntry zipEntry = new ZipEntry(nombreArchivo);
                    zipOut.putNextEntry(zipEntry);

                    ByteArrayOutputStream archivoStream = new ByteArrayOutputStream();
                    sftp.get(rutaArchivo, archivoStream);
                    zipOut.write(archivoStream.toByteArray());
                    zipOut.closeEntry();

                } catch (Exception e) {
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
}