package com.biblosmast.controller;

import com.biblosmast.model.dto.ConfiguracionRequest;
import com.biblosmast.model.dto.ConfiguracionResponse;
import com.biblosmast.service.IConfiguracionService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final IConfiguracionService configuracionService;

    @GetMapping
    public List<ConfiguracionResponse> listar() {
        return configuracionService.listar();
    }

    @PutMapping("/{clave}")
    public ConfiguracionResponse actualizar(@PathVariable String clave,
                                            @Valid @RequestBody ConfiguracionRequest request) {
        return configuracionService.actualizar(clave, request);
    }
}
