package com.biblosmast.model.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class EjemplarResponse {
    private Integer idEjemplar;
    private Integer idLibro;
    private String titulo;
    private String isbn;
    private String codigo;
    private String ubicacion;
    private String estado;
    private LocalDate fechaIngreso;
}
