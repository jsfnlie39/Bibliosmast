package com.biblosmast.controller;

import com.biblosmast.model.dto.LibroRequest;
import com.biblosmast.model.dto.LibroResponse;
import com.biblosmast.service.ILibroService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class LibroController {

    private final ILibroService libroService;

    @GetMapping
    public List<LibroResponse> listar(@RequestParam(name = "q", required = false) String q) {
        return libroService.listar(q);
    }

    @GetMapping("/{id}")
    public LibroResponse obtener(@PathVariable Integer id) {
        return libroService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibroResponse crear(@Valid @RequestBody LibroRequest request) {
        return libroService.crear(request);
    }

    @PutMapping("/{id}")
    public LibroResponse actualizar(@PathVariable Integer id, @Valid @RequestBody LibroRequest request) {
        return libroService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        libroService.eliminar(id);
    }
}
