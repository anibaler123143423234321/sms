package com.midas.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuscarAudiosVentaRequest {
    private String numeroServidor;
    private String fechaRegistro;  // Formato ISO: "2025-10-15T21:54:06"
    private String movilContacto;
    private String numeroAgente;   // Opcional: "8009", "8022", etc. (código del agente)
}

