package com.biblosmast.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class ConfiguracionResponse {
    private String clave;
    private BigDecimal valor;
    private String descripcion;
}
