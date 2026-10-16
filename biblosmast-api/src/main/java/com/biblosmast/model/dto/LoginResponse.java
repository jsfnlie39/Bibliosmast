package com.biblosmast.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Nunca incluye el password ni su hash. */
@Getter @AllArgsConstructor
public class LoginResponse {
    private Integer idUsuario;
    private String username;
    private String nombres;
    private String apellidos;
    private String rol;
}
