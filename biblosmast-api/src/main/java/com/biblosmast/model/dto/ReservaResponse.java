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
    /** PENDIENTE, LISTA, VENCIDA, ATENDIDA o CANCELADA (PENDIENTE con fecha pasada se muestra como VENCIDA). */
    private String estado;
    /** Ejemplares del libro que están DISPONIBLES en este momento. */
    private long ejemplaresDisponibles;
    /** Turno en la cola del libro (1 = el siguiente en recibir un ejemplar). Solo para reservas PENDIENTE vigentes. */
    private Integer posicionCola;
    /** Solo en LISTA: último día para recoger el ejemplar apartado. */
    private LocalDate fechaLimiteRecogida;
    /** Solo en LISTA: código del ejemplar apartado. */
    private String codigoEjemplar;
    /** Si la reserva todavía se puede editar (PENDIENTE o LISTA). */
    private boolean editable;
}
