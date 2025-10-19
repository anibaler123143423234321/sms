package com.midas.sms.dto;

import java.util.List;

public record PaginaContenidoDTO(
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        List<?> contenido  // Puede ser List<ArchivoSistemaDTO> o List<AudioVentaDTO>
) {}