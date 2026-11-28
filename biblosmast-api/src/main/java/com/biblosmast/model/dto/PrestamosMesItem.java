package com.biblosmast.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class PrestamosMesItem {
    /** Mes en formato AAAA-MM. */
    private String mes;
    private long prestamos;
}
