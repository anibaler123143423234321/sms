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
    private String nombre;                  // Nombre del archivo de audio
    private String fechaCreadaRutaServidor; // Fecha cuando se creó la carpeta en el servidor (ejecución del script)
    private String hora;                    // Hora del archivo
    private String tamano;                  // Tamaño formateado (KB/MB)
    private String tamanoBytes;             // Tamaño en bytes (para cálculos)
    private String duracion;                // Duración del audio en formato mm:ss
    private String ipServidor;              // IP del servidor donde se encontró
    private Long idLeadTranscrito;          // ID del lead (cliente_residencial) asociado a esta transcripción

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioVentaDTO that = (AudioVentaDTO) o;
        return Objects.equals(nombre, that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }
}

