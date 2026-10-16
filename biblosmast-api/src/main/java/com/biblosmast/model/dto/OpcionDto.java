package com.biblosmast.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Par id/nombre para llenar listas desplegables en el frontend. */
@Getter @AllArgsConstructor
public class OpcionDto {
    private Integer id;
    private String nombre;
}
