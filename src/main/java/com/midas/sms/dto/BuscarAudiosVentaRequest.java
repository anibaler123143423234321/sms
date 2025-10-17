package com.midas.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuscarAudiosVentaRequest {
    private String numeroServidor;
    private String fechaRegistro;  // Formato ISO: "2025-10-15T21:54:06"
    private String movilContacto;
    private String numeroAgente;   // Opcional: "009", "022", etc.
}

