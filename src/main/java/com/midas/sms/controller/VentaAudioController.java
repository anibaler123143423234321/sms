package com.midas.sms.controller;

import com.midas.sms.dto.BuscarAudiosVentaRequest;
import com.midas.sms.dto.BuscarAudiosVentaResponse;
import com.midas.sms.service.VentaAudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaAudioController {

    private final VentaAudioService ventaAudioService;

    /**
     * Busca los audios de una venta en el servidor de archivos
     *
     * POST /api/ventas/buscar-audios
     * Body: {
     *   "numeroServidor": "23",
     *   "fechaRegistro": "2025-10-15T21:54:06",
     *   "movilContacto": "987654321",
     *   "numeroAgente": "009"  // Opcional
     * }
     */
    @PostMapping("/buscar-audios")
    public ResponseEntity<BuscarAudiosVentaResponse> buscarAudios(@RequestBody BuscarAudiosVentaRequest request) {
        log.info("📥 POST /api/ventas/buscar-audios - Servidor: {}, Móvil: {}", 
            request.getNumeroServidor(), request.getMovilContacto());

        BuscarAudiosVentaResponse response = ventaAudioService.buscarAudiosVenta(request);

        if (response.isSuccess()) {
            log.info("✅ Búsqueda exitosa - {} audios encontrados", response.getTotalAudios());
            return ResponseEntity.ok(response);
        } else {
            log.warn("⚠️ Búsqueda sin resultados: {}", response.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}

