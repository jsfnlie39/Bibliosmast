package com.biblosmast.controller;

import com.biblosmast.model.dto.ReservaRequest;
import com.biblosmast.model.dto.ReservaResponse;
import com.biblosmast.service.IReservaService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final IReservaService reservaService;

    @GetMapping
    public List<ReservaResponse> listar(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "idUsuario", required = false) Integer idUsuario) {
        return reservaService.listar(estado, idUsuario);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Integer id) {
        return reservaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request) {
        return reservaService.crear(request);
    }

    @PutMapping("/{id}")
    public ReservaResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ReservaRequest request) {
        return reservaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        reservaService.eliminar(id);
    }
}
