package com.biblosmast.controller;

import com.biblosmast.model.dto.LibroFiltro;
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

    /** Todos los filtros son opcionales y se combinan: ?q=&idAutor=&idCategoria=&idEditorial=&idioma=&anioDesde=&anioHasta=&soloDisponibles= */
    @GetMapping
    public List<LibroResponse> listar(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "idAutor", required = false) Integer idAutor,
            @RequestParam(name = "idCategoria", required = false) Integer idCategoria,
            @RequestParam(name = "idEditorial", required = false) Integer idEditorial,
            @RequestParam(name = "idioma", required = false) String idioma,
            @RequestParam(name = "anioDesde", required = false) Integer anioDesde,
            @RequestParam(name = "anioHasta", required = false) Integer anioHasta,
            @RequestParam(name = "soloDisponibles", defaultValue = "false") boolean soloDisponibles) {
        return libroService.buscar(LibroFiltro.builder()
                .q(q).idAutor(idAutor).idCategoria(idCategoria).idEditorial(idEditorial).idioma(idioma)
                .anioDesde(anioDesde).anioHasta(anioHasta).soloDisponibles(soloDisponibles)
                .build());
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
