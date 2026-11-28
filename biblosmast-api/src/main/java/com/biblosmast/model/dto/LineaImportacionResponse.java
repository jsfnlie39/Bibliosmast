package com.biblosmast.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LineaImportacionResponse {
    /** Línea del archivo (la 1 es el encabezado). */
    private int numeroLinea;
    private String isbn;
    private String titulo;
    /** Vacía si la línea está bien. */
    private List<String> errores;
}
