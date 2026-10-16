package com.biblosmast.model.dto;

import com.biblosmast.model.entity.EstadoUsuario;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UsuarioRequest {

    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El documento solo admite letras y números")
    private String documento;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
    private String apellidos;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    private String email;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    @NotNull(message = "El estado es obligatorio")
    private EstadoUsuario estado;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 40, message = "El nombre de usuario debe tener entre 4 y 40 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_.]+$", message = "El nombre de usuario solo admite letras, números, punto y guion bajo")
    private String username;

    /** Obligatoria al crear; al editar, si viene vacía no se cambia. */
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Integer idRol;

    private Boolean activo;
}
