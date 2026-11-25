package com.biblosmast.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class ReservaResponse {
    private Integer idReserva;
    private Integer idUsuario;
    private String usuario;
    private String documento;
    private Integer idLibro;
    private String titulo;
    private String isbn;
    private LocalDateTime fechaReserva;
    private LocalDate fechaVencimiento;
    /** PENDIENTE, VENCIDA (pendiente con fecha pasada), ATENDIDA o CANCELADA. */
    private String estado;
    /** Ejemplares del libro que están DISPONIBLES en este momento. */
    private long ejemplaresDisponibles;
}
