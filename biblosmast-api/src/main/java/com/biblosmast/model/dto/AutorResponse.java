package com.biblosmast.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class AutorResponse {
    private Integer idAutor;
    private String nombres;
    private String apellidos;
    private String nacionalidad;
}
