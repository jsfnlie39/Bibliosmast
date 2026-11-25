package com.biblosmast.service;

import com.biblosmast.model.dto.ConfiguracionRequest;
import com.biblosmast.model.dto.ConfiguracionResponse;
import com.biblosmast.model.entity.Configuracion;
import com.biblosmast.repository.ConfiguracionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConfiguracionService implements IConfiguracionService {

    public static final String CLAVE_MULTA_RETRASO = "multa_por_dia_retraso";
    public static final String CLAVE_MULTA_DANO = "multa_dano";
    public static final String CLAVE_MULTA_PERDIDA = "multa_perdida";

    private final ConfiguracionRepository configuracionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionResponse> listar() {
        return configuracionRepository.findAll().stream()
                .sorted(Comparator.comparing(Configuracion::getClave))
                .map(this::aRespuesta)
                .toList();
    }

    @Override
    @Transactional
    public ConfiguracionResponse actualizar(String clave, ConfiguracionRequest request) {
        Configuracion configuracion = configuracionRepository.findById(clave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuración no encontrada"));
        configuracion.setValor(request.getValor().setScale(2, RoundingMode.HALF_UP));
        return aRespuesta(configuracionRepository.save(configuracion));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal valor(String clave) {
        return configuracionRepository.findById(clave)
                .map(Configuracion::getValor)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta la configuración '" + clave + "' en la base de datos"));
    }

    private ConfiguracionResponse aRespuesta(Configuracion c) {
        return new ConfiguracionResponse(c.getClave(), c.getValor(), c.getDescripcion());
    }
}
