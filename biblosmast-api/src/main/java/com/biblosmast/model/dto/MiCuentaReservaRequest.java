package com.biblosmast.model.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Reservar desde «Mi cuenta»: la persona sale del token y el vencimiento lo define la configuración. */
@Getter @Setter
public class MiCuentaReservaRequest {

    @NotNull(message = "Selecciona el libro")
    private Integer idLibro;
}
