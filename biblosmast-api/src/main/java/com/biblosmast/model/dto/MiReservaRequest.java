package com.biblosmast.model.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Reserva hecha por la propia persona desde «Mi cuenta»: el usuario sale del token, no del cuerpo. */
@Getter @Setter
public class MiReservaRequest {

    @NotNull(message = "Selecciona el libro")
    private Integer idLibro;
}
