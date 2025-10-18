package com.midas.sms.controller;

import com.midas.sms.dto.PaginaContenidoDTO;
import com.midas.sms.service.ServidorCixSolivesa2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/monitor-cix-solivesa2")
@RequiredArgsConstructor
public class ServidorCixSolivesa2Controller {

    private final ServidorCixSolivesa2Service servidorCixSolivesa2Service;

    @GetMapping("/archivos")
    public ResponseEntity<PaginaContenidoDTO> listarArchivos(
            @RequestParam(required = false, defaultValue = "") String subRuta,
            @RequestParam(required = false, defaultValue = "") String terminoBusqueda,
            @RequestParam(required = false, defaultValue = "") String fechaDesde,
            @RequestParam(required = false, defaultValue = "") String fechaHasta,
            @RequestParam(required = false, defaultValue = "1") int pagina,
            @RequestParam(required = false, defaultValue = "50") int tamano) {

        PaginaContenidoDTO resultado = servidorCixSolivesa2Service.listarContenidoPaginado(
                subRuta, terminoBusqueda, fechaDesde, fechaHasta, pagina, tamano);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/archivos/{carpeta}")
    public ResponseEntity<PaginaContenidoDTO> listarArchivosCarpeta(
            @PathVariable String carpeta,
            @RequestParam(required = false, defaultValue = "") String terminoBusqueda,
            @RequestParam(required = false, defaultValue = "") String fechaDesde,
            @RequestParam(required = false, defaultValue = "") String fechaHasta,
            @RequestParam(required = false, defaultValue = "1") int pagina,
            @RequestParam(required = false, defaultValue = "50") int tamano) {

        PaginaContenidoDTO resultado = servidorCixSolivesa2Service.listarContenidoPaginado(
                carpeta, terminoBusqueda, fechaDesde, fechaHasta, pagina, tamano);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/descargar")
    public ResponseEntity<InputStreamResource> descargarArchivo(
            @RequestParam String ruta,
            @RequestParam String nombreArchivo) {

        InputStream inputStream = servidorCixSolivesa2Service.descargarArchivo(ruta, nombreArchivo);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    @PostMapping("/descargar-multiples")
    public ResponseEntity<InputStreamResource> descargarMultiplesArchivos(
            @RequestParam String ruta,
            @RequestBody List<String> nombresArchivos) {

        InputStream zipStream = servidorCixSolivesa2Service.descargarArchivosComoZip(ruta, nombresArchivos);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archivos.zip\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(zipStream));
    }
}

