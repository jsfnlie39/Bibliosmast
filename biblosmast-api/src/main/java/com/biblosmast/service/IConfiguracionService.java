package com.biblosmast.service;

import com.biblosmast.model.dto.ConfiguracionRequest;
import com.biblosmast.model.dto.ConfiguracionResponse;
import java.math.BigDecimal;
import java.util.List;

public interface IConfiguracionService {
    List<ConfiguracionResponse> listar();
    ConfiguracionResponse actualizar(String clave, ConfiguracionRequest request);
    /** Valor de una clave de configuración (error si la clave no existe en la base). */
    BigDecimal valor(String clave);
}
