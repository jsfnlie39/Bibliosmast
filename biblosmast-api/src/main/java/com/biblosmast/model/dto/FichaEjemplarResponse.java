package com.biblosmast.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Seguimiento de una copia física: datos, estado actual y cada vez que se prestó, con cómo volvió. Solo personal. */
@Getter @Builder
public class FichaEjemplarResponse {
    private EjemplarResponse ejemplar;
    private long totalPrestamos;
    private long prestamosSinDevolver;
    /** Cuántas veces volvió con desgaste, dañado o perdido (las que dejan huella en el estado del ejemplar). */
    private long vecesRegular;
    private long vecesDanado;
    private long vecesPerdido;
    /** Del préstamo más reciente al más antiguo. */
    private List<MovimientoLibroResponse> prestamos;
}
