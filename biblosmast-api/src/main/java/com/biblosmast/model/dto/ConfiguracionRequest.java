package com.biblosmast.model.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConfiguracionRequest {

    @NotNull(message = "El valor es obligatorio")
    @DecimalMin(value = "0", message = "El valor no puede ser negativo")
    @DecimalMax(value = "1000000", message = "El valor no puede superar 1.000.000")
    private BigDecimal valor;
}
