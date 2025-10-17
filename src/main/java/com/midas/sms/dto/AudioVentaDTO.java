package com.midas.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioVentaDTO {
    private String nombre;
    private String fecha;
    private String hora;
    private String tamano;
    private String urlCompleta;
    private String numeroAgente;
    private String extension;
}

