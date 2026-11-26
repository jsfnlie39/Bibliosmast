package com.biblosmast.model.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Todo lo de una persona: sus préstamos, reservas y multas. */
@Getter @Builder
public class MiCuentaResponse {
    private Integer idUsuario;
    private List<PrestamoResponse> prestamos;
    private List<ReservaResponse> reservas;
    private List<MultaResponse> multas;
    /** Suma de las multas que todavía no se han pagado. */
    private BigDecimal totalMultasPendientes;
}
