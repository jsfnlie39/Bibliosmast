package com.biblosmast.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CambioClaveRequest {

    @NotBlank(message = "Escribe tu contraseña actual")
    @Size(max = 72, message = "La contraseña no puede superar 72 caracteres")
    private String claveActual;

    @NotBlank(message = "Escribe la nueva contraseña")
    @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres")
    private String claveNueva;
}
