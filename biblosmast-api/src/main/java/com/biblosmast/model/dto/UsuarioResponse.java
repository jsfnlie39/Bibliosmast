package com.biblosmast.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Nunca incluye la contraseña ni su hash. */
@Getter @Builder
public class UsuarioResponse {
    private Integer idUsuario;
    private String documento;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    private String estado;
    private String username;
    private Integer idRol;
    private String rol;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}
