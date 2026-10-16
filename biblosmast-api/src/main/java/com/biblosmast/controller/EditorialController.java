package com.biblosmast.controller;

import com.biblosmast.model.dto.EditorialRequest;
import com.biblosmast.model.dto.EditorialResponse;
import com.biblosmast.service.IEditorialService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/editoriales")
@RequiredArgsConstructor
public class EditorialController {

    private final IEditorialService service;

    @GetMapping
    public List<EditorialResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public EditorialResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EditorialResponse crear(@Valid @RequestBody EditorialRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public EditorialResponse actualizar(@PathVariable Integer id, @Valid @RequestBody EditorialRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }
}
