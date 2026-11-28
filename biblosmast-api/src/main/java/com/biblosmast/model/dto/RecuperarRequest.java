package com.biblosmast.model.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecuperarRequest {

    @NotBlank(message = "Escribe tu correo")
    @Email(message = "El correo no es válido")
    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    private String correo;
}
