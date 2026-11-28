package com.biblosmast.controller;

import com.biblosmast.model.dto.AutorRequest;
import com.biblosmast.model.dto.AutorResponse;
import com.biblosmast.service.IAutorService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autores")
@RequiredArgsConstructor
public class AutorController {

    private final IAutorService service;

    @GetMapping
    public List<AutorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AutorResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AutorResponse crear(@Valid @RequestBody AutorRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public AutorResponse actualizar(@PathVariable Integer id, @Valid @RequestBody AutorRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }
}
