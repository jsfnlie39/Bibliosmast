package com.biblosmast.controller;

import com.biblosmast.model.dto.MultaResponse;
import com.biblosmast.service.IMultaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/multas")
@RequiredArgsConstructor
public class MultaController {

    private final IMultaService multaService;

    @GetMapping
    public List<MultaResponse> listar(@RequestParam(name = "estado", required = false) String estado,
                                      @RequestParam(name = "idUsuario", required = false) Integer idUsuario) {
        return multaService.listar(estado, idUsuario);
    }

    @GetMapping("/{id}")
    public MultaResponse obtener(@PathVariable Integer id) {
        return multaService.obtener(id);
    }

    @PutMapping("/{id}/pago")
    public MultaResponse pagar(@PathVariable Integer id) {
        return multaService.pagar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anular(@PathVariable Integer id) {
        multaService.anular(id);
    }
}
