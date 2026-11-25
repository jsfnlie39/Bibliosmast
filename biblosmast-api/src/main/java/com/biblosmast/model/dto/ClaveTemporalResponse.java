package com.biblosmast.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Se muestra una sola vez al administrador, para que se la entregue al usuario. */
@Getter @AllArgsConstructor
public class ClaveTemporalResponse {
    private String username;
    private String claveTemporal;
}
