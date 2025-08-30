package com.midas.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {
    
    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipoDocumento;
    
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 8, max = 20, message = "El número de documento debe tener entre 8 y 20 caracteres")
    private String numeroDocumento;
    
    @NotBlank(message = "Los apellidos y nombres son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos y nombres deben tener entre 2 y 100 caracteres")
    private String apellidosNombres;
    
    @NotBlank(message = "El número de celular es obligatorio")
    @Pattern(regexp = "^\\+[0-9]{1,4}[0-9]{9,15}$", message = "El número de celular debe incluir prefijo internacional (+34, +1, etc.) y tener entre 9 y 15 dígitos")
    @Size(max = 20, message = "El número de celular no puede exceder 20 caracteres")
    private String numeroCelular;
}