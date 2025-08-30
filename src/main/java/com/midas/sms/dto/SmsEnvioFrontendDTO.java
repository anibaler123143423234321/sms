package com.midas.sms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsEnvioFrontendDTO {
    
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre del cliente debe tener entre 2 y 200 caracteres")
    private String nombreCliente;
    
    @NotBlank(message = "El número de teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El número debe tener exactamente 9 dígitos (sin prefijo)")
    private String numeroTelefono;
    
    @NotBlank(message = "La campaña es obligatoria")
    @Size(max = 100, message = "La campaña no puede exceder 100 caracteres")
    private String campania;
    
    @NotBlank(message = "El tipo de SMS es obligatorio")
    @Size(max = 100, message = "El tipo de SMS no puede exceder 100 caracteres")
    private String tipoSms;
    
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(min = 10, max = 800, message = "El mensaje debe tener entre 10 y 800 caracteres")
    private String mensaje;
    
    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendAt;
    
    // Constructor para facilitar la creación desde el frontend
    public SmsEnvioFrontendDTO(String nombreCliente, String numeroTelefono, 
                              String campania, String tipoSms, String mensaje, Long userId) {
        this.nombreCliente = nombreCliente;
        this.numeroTelefono = numeroTelefono;
        this.campania = campania;
        this.tipoSms = tipoSms;
        this.mensaje = mensaje;
        this.userId = userId;
        this.sendAt = LocalDateTime.now(); // Envío inmediato por defecto
    }
}