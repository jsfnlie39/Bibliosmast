package com.biblosmast.model.dto;

import com.biblosmast.model.entity.EstadoReserva;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReservaRequest {

    @NotNull(message = "Selecciona el usuario que reserva")
    private Integer idUsuario;

    @NotNull(message = "Selecciona el libro")
    private Integer idLibro;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    /** Opcional. Al crear siempre queda PENDIENTE; al editar: PENDIENTE, ATENDIDA o CANCELADA (VENCIDA se calcula sola). */
    private EstadoReserva estado;
}
