package com.biblosmast.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class NotificacionResponse {
    private Integer idNotificacion;
    /** POR_VENCER, VENCIDO o RESERVA_LISTA. */
    private String tipo;
    private String mensaje;
    private LocalDateTime fechaCreacion;
    private boolean leida;
}
