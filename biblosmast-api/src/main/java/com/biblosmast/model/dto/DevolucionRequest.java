package com.biblosmast.model.dto;

import com.biblosmast.model.entity.EstadoDevolucion;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DevolucionRequest {

    @NotNull(message = "Indica el estado en que se devuelve el ejemplar")
    private EstadoDevolucion estadoDevolucion;
}
