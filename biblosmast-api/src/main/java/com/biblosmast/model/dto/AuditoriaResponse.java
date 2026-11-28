package com.biblosmast.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AuditoriaResponse {
    private Integer idAuditoria;
    private Integer idUsuario;
    /** Nombre de quien hizo la acción; null si no se conoce. */
    private String usuario;
    private String accion;
    private String tabla;
    private Integer idRegistro;
    private String detalle;
    private LocalDateTime fecha;
}
