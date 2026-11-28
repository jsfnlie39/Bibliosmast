package com.biblosmast.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Un ejemplar del libro que se prestó: quién lo tuvo y cuándo. */
@Getter @Builder
public class MovimientoLibroResponse {
    private Integer idPrestamo;
    private Integer idEjemplar;
    private String codigoEjemplar;
    private Integer idUsuario;
    private String usuario;
    private String documento;
    private LocalDateTime fechaPrestamo;
    private LocalDate fechaLimite;
    /** Null mientras el ejemplar no se ha devuelto. */
    private LocalDateTime fechaDevolucion;
    /** BUEN_ESTADO, DANADO o PERDIDO; null si no se ha devuelto. */
    private String estadoDevolucion;
    /** Sin devolver y con la fecha límite pasada. */
    private boolean vencido;
}
