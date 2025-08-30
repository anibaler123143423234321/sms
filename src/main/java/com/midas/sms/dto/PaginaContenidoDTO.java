package com.midas.sms.dto;

import java.util.List;

public record PaginaContenidoDTO(
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        List<ArchivoSistemaDTO> contenido
) {}