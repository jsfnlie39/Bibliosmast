package com.biblosmast.controller;

import com.biblosmast.model.dto.MiReservaRequest;
import com.biblosmast.model.dto.MultaResponse;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.dto.ReservaResponse;
import com.biblosmast.service.IMiCuentaService;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** «Mi cuenta»: préstamos, reservas y multas de la persona con sesión. La persona sale siempre del token. */
@RestController
@RequestMapping("/api/mi-cuenta")
@RequiredArgsConstructor
public class MiCuentaController {

    private final IMiCuentaService miCuentaService;

    @GetMapping("/prestamos")
    public List<PrestamoResponse> prestamos(Principal principal) {
        return miCuentaService.prestamos(principal.getName());
    }

    @GetMapping("/reservas")
    public List<ReservaResponse> reservas(Principal principal) {
        return miCuentaService.reservas(principal.getName());
    }

    @GetMapping("/multas")
    public List<MultaResponse> multas(Principal principal) {
        return miCuentaService.multas(principal.getName());
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse reservar(Principal principal, @Valid @RequestBody MiReservaRequest request) {
        return miCuentaService.reservar(principal.getName(), request.getIdLibro());
    }

    @PutMapping("/reservas/{id}/cancelar")
    public ReservaResponse cancelarReserva(Principal principal, @PathVariable Integer id) {
        return miCuentaService.cancelarReserva(principal.getName(), id);
    }
}
