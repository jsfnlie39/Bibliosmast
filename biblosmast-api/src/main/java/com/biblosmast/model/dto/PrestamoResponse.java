package com.biblosmast.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class PrestamoResponse {
    private Integer idPrestamo;
    private Integer idUsuario;
    private String usuario;
    private String documento;
    private String bibliotecario;
    private LocalDateTime fechaPrestamo;
    private LocalDate fechaLimite;
    /** ACTIVO, VENCIDO (activo con la fecha límite pasada) o FINALIZADO. */
    private String estado;
    private long diasRetraso;
    private String observaciones;
    private List<DetallePrestamoResponse> detalles;
    /** Reservas del usuario que este préstamo atendió. Solo se llena al registrar el préstamo; en las consultas es 0. */
    private int reservasAtendidas;
}
