package com.biblosmast.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class UsuarioRetrasoItem {
    private Integer idUsuario;
    private String usuario;
    private String documento;
    /** Multas por retraso que le pusieron en el periodo. */
    private long multasRetraso;
    private BigDecimal valorMultasRetraso;
    /** Ejemplares que tiene ahora mismo sin devolver y con la fecha límite pasada (todavía sin multa). */
    private long ejemplaresVencidosAhora;
}
