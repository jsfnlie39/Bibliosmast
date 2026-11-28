package com.biblosmast.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class PerfilResponse {
    private Integer idUsuario;
    private String username;
    private String rol;
    private String documento;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    private boolean debeCambiarClave;
}
