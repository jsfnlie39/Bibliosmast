package com.biblosmast.controller;

import com.biblosmast.model.dto.EjemplarRequest;
import com.biblosmast.model.dto.EjemplarResponse;
import com.biblosmast.service.IEjemplarService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ejemplares")
@RequiredArgsConstructor
public class EjemplarController {

    private final IEjemplarService ejemplarService;

    @GetMapping
    public List<EjemplarResponse> listar(@RequestParam(name = "idLibro", required = false) Integer idLibro) {
        return ejemplarService.listar(idLibro);
    }

    @GetMapping("/{id}")
    public EjemplarResponse obtener(@PathVariable Integer id) {
        return ejemplarService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EjemplarResponse crear(@Valid @RequestBody EjemplarRequest request) {
        return ejemplarService.crear(request);
    }

    @PutMapping("/{id}")
    public EjemplarResponse actualizar(@PathVariable Integer id, @Valid @RequestBody EjemplarRequest request) {
        return ejemplarService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        ejemplarService.eliminar(id);
    }
}
