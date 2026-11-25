package com.biblosmast.controller;

import com.biblosmast.model.dto.DevolucionRequest;
import com.biblosmast.model.dto.PrestamoRequest;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.service.IPrestamoService;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final IPrestamoService prestamoService;

    @GetMapping
    public List<PrestamoResponse> listar(@RequestParam(name = "estado", required = false) String estado,
                                         @RequestParam(name = "idUsuario", required = false) Integer idUsuario) {
        return prestamoService.listar(estado, idUsuario);
    }

    @GetMapping("/{id}")
    public PrestamoResponse obtener(@PathVariable Integer id) {
        return prestamoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrestamoResponse crear(@Valid @RequestBody PrestamoRequest request) {
        return prestamoService.crear(request);
    }

    @PutMapping("/{idPrestamo}/detalles/{idDetalle}/devolucion")
    public PrestamoResponse devolver(@PathVariable Integer idPrestamo,
                                     @PathVariable Integer idDetalle,
                                     @Valid @RequestBody DevolucionRequest request) {
        return prestamoService.devolver(idPrestamo, idDetalle, request);
    }
}
