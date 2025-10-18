package com.midas.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

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
    private String urlMp3; // URL del audio convertido a MP3
    private String estadoConversion; // "PENDIENTE", "CONVIRTIENDO", "COMPLETADO", "ERROR"
    private String ruta; // Ruta del archivo en el servidor (ej: "GSM/spain/celulares/16102025")
    private String controlador; // Endpoint del controlador (ej: "/api/monitor-cix-maryory-josey")

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioVentaDTO that = (AudioVentaDTO) o;
        return Objects.equals(nombre, that.nombre) && Objects.equals(urlCompleta, that.urlCompleta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, urlCompleta);
    }
}

