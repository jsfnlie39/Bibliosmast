package com.biblosmast.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MultaResponse {
    private Integer idMulta;
    private Integer idPrestamo;
    private Integer idUsuario;
    private String usuario;
    private String documento;
    private BigDecimal valor;
    /** RETRASO, DANO o PERDIDA. */
    private String motivo;
    /** PENDIENTE o PAGADA. */
    private String estado;
    private LocalDateTime fecha;
    private LocalDateTime fechaPago;
}
