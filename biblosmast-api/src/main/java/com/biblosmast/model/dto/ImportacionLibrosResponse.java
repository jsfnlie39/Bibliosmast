package com.biblosmast.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class ImportacionLibrosResponse {
    /** true si solo se revisó el archivo. */
    private boolean soloValidar;
    /** true si los libros se guardaron. Nunca se guarda nada si alguna línea tiene errores. */
    private boolean importado;
    private int totalLineas;
    private int lineasValidas;
    private int lineasConErrores;
    private int librosCreados;
    private String separador;
    private List<LineaImportacionResponse> lineas;
}
