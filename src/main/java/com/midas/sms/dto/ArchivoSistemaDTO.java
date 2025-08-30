package com.midas.sms.dto;

public record ArchivoSistemaDTO(
        String nombre,
        String fecha,
        String hora,
        String tamano,
        boolean esDirectorio
) {}