package com.biblosmast.controller;

import com.biblosmast.model.dto.ImportacionLibrosResponse;
import com.biblosmast.model.dto.ImportarLibrosRequest;
import com.biblosmast.service.IImportacionLibrosService;
import java.security.Principal;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Importar libros desde CSV. Solo ADMIN y BIBLIOTECARIO (regla general de /api/libros). */
@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class ImportacionLibrosController {

    private final IImportacionLibrosService importacionService;

    @PostMapping("/importar")
    public ImportacionLibrosResponse importar(@Valid @RequestBody ImportarLibrosRequest request, Principal principal) {
        return importacionService.importar(request, principal.getName());
    }
}
