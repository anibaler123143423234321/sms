package com.midas.sms.dto;

import java.time.Instant;

public record ServidorResponseDTO(
        Long id,
        String nombre,
        String descripcion,
        Instant createdAt
) {}