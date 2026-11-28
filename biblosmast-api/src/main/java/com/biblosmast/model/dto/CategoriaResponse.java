package com.biblosmast.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class CategoriaResponse {
    private Integer idCategoria;
    private String nombre;
    private String descripcion;
}
