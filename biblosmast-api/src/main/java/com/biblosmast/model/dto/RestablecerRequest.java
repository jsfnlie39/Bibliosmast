package com.biblosmast.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RestablecerRequest {

    @NotBlank(message = "Falta el código del enlace")
    @Size(max = 100, message = "El código no es válido")
    private String token;

    @NotBlank(message = "Escribe la nueva contraseña")
    @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres")
    private String claveNueva;
}
