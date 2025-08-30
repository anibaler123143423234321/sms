package com.midas.sms.dto;

import java.util.List;

public record PaginaArchivosDTO(
        int paginaActual,
        int totalPaginas,
        long totalArchivos,
        List<ArchivoDTO> archivos
) {}