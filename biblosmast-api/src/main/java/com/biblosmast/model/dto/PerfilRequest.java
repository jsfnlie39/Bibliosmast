package com.biblosmast.model.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Lo que cada persona puede cambiar de sí misma: correo y teléfono. Nombre, documento y rol los cambia el administrador. */
@Getter @Setter
public class PerfilRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    private String email;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;
}
