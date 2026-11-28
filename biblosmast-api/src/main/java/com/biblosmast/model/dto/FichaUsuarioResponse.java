package com.biblosmast.model.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Ficha de un usuario: datos, cupo según su tipo, y su historial de préstamos, reservas y multas. Solo personal. */
@Getter @Builder
public class FichaUsuarioResponse {
    private UsuarioResponse usuario;
    private Integer maxLibros;
    private Integer diasPrestamo;
    private Integer maxRenovaciones;
    /** Ejemplares que tiene prestados y no ha devuelto (cuentan contra {@code maxLibros}). */
    private long ejemplaresSinDevolver;
    private long multasPendientes;
    private BigDecimal valorMultasPendientes;
    private List<PrestamoResponse> prestamos;
    private List<ReservaResponse> reservas;
    private List<MultaResponse> multas;
}
