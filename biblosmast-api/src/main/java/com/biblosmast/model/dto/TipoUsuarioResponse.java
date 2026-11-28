package com.biblosmast.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class TipoUsuarioResponse {
    private Integer idTipoUsuario;
    private String nombre;
    private Integer maxLibros;
    private Integer diasPrestamo;
    private Integer maxRenovaciones;
    /** Cuántos usuarios tienen este tipo, incluidos los dados de baja que conservan su historial (un tipo en uso no se elimina). */
    private long cantidadUsuarios;
}
