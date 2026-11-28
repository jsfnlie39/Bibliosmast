package com.biblosmast.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Ficha de un libro: datos, ejemplares, historial de préstamos y reservas. Solo personal. */
@Getter @Builder
public class FichaLibroResponse {
    private LibroResponse libro;
    private List<EjemplarResponse> ejemplares;
    /** Veces que se ha prestado un ejemplar de este libro (cada ejemplar cuenta). */
    private long totalPrestamos;
    private long prestamosSinDevolver;
    private List<MovimientoLibroResponse> prestamos;
    private List<ReservaResponse> reservas;
}
