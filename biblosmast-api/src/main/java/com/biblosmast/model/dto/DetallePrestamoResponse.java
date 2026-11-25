package com.biblosmast.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class DetallePrestamoResponse {
    private Integer idDetalle;
    private Integer idEjemplar;
    private String codigo;
    private String titulo;
    private LocalDateTime fechaDevolucion;
    private String estadoDevolucion;
}
