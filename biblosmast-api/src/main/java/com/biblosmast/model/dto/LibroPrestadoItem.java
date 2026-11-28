package com.biblosmast.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LibroPrestadoItem {
    private Integer idLibro;
    private String titulo;
    /** Veces que se prestó un ejemplar de este libro en el periodo (cada ejemplar cuenta). */
    private long prestamos;
}
