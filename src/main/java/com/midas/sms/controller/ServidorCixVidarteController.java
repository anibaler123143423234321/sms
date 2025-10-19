package com.midas.sms.controller;

import com.midas.sms.dto.PaginaContenidoDTO;
import com.midas.sms.service.ServidorCixVidarteService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/monitor-cix-vidarte")
@RequiredArgsConstructor
public class ServidorCixVidarteController {

    private final ServidorCixVidarteService service;

    /**
     * Endpoint único y flexible para explorar el contenido de CUALQUIER carpeta.
     * Utiliza el parámetro 'ruta' para navegar por las subcarpetas.
     */
    @GetMapping("/archivos")
    public ResponseEntity<PaginaContenidoDTO> explorarContenido(
            @RequestParam(required = false, defaultValue = "") String ruta,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "100") int tamano
    ) {
        PaginaContenidoDTO resultado = service.listarContenidoPaginado(ruta, buscar, fechaDesde, fechaHasta, pagina, tamano);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Lista el contenido de una subcarpeta específica (ej. "GSM").
     */
    @GetMapping("/archivos/{carpeta}")
    public ResponseEntity<PaginaContenidoDTO> listarContenidoDeCarpeta(
            @PathVariable String carpeta,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "100") int tamano
    ) {
        PaginaContenidoDTO resultado = service.listarContenidoPaginado(carpeta, buscar, fechaDesde, fechaHasta, pagina, tamano);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Descarga un archivo individual
     */
    @GetMapping("/descargar")
    public ResponseEntity<InputStreamResource> descargarArchivo(
            @RequestParam String ruta,
            @RequestParam String archivo
    ) {
        try {
            InputStream inputStream = service.descargarArchivo(ruta, archivo);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Descarga múltiples archivos como ZIP
     */
    @PostMapping("/descargar-multiples")
    public ResponseEntity<InputStreamResource> descargarArchivosMultiples(
            @RequestParam String ruta,
            @RequestBody List<String> archivos
    ) {
        try {
            InputStream zipStream = service.descargarArchivosComoZip(ruta, archivos);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archivos_seleccionados.zip\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(zipStream));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ NUEVO ENDPOINT - Búsqueda rápida de audios usando script optimizado
     *
     * GET /api/monitor-cix-vidarte/buscar-rapido?numeroMovil=653608842&pagina=1&tamano=50
     *
     * Usa el script /BUSQUEDA/buscar_audios2 para búsqueda ultra-rápida
     * - Ejecuta script que crea carpeta: /BUSQUEDA/audios/{fecha-actual}/{numeroMovil}/
     * - Filtra automáticamente archivos con duración > 3 minutos (> 180 KB)
     * - Ordena por fecha descendente (más recientes primero)
     * - Retorna resultados paginados
     *
     * @param numeroMovil Número de móvil a buscar (ej: 653608842)
     * @param pagina Número de página (default: 1)
     * @param tamano Tamaño de página (default: 50)
     * @return Página con los audios encontrados
     */
    @GetMapping("/buscar-rapido")
    public ResponseEntity<PaginaContenidoDTO> buscarAudiosRapido(
            @RequestParam String numeroMovil,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int tamano
    ) {
        PaginaContenidoDTO resultado = service.buscarAudiosRapido(numeroMovil, pagina, tamano);
        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/eliminar-audios")
    public ResponseEntity<Void> eliminarCarpetaAudios(
            @RequestParam String numeroMovil,
            @RequestParam String fecha
    ) {
        service.eliminarCarpetaAudios(numeroMovil, fecha);
        return ResponseEntity.ok().build();
    }
}