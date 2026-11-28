package com.biblosmast.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Nunca incluye el password ni su hash. Incluye el token JWT que el frontend debe enviar en cada petición. */
@Getter @AllArgsConstructor
public class LoginResponse {
    private Integer idUsuario;
    private String username;
    private String nombres;
    private String apellidos;
    private String rol;
    /** El JWT firmado. */
    private String token;
    /** Siempre "Bearer": así se envía en el encabezado Authorization. */
    private String tipoToken;
    /** Momento en que el token vence, en segundos desde 1970. */
    private long expiraEn;
    /** True si tiene una clave temporal: el frontend la lleva a cambiarla antes de seguir. */
    private boolean debeCambiarClave;
}
