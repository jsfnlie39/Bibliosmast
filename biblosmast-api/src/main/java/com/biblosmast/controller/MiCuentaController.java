package com.biblosmast.controller;

import com.biblosmast.model.dto.MiCuentaReservaRequest;
import com.biblosmast.model.dto.MiCuentaResponse;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.dto.ReservaResponse;
import com.biblosmast.service.IMiCuentaService;
import java.security.Principal;
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

/** «Mi cuenta»: préstamos, reservas y multas de quien tiene la sesión. La persona sale siempre del token. */
@RestController
@RequestMapping("/api/mi-cuenta")
@RequiredArgsConstructor
public class MiCuentaController {

    private final IMiCuentaService miCuentaService;

    @GetMapping
    public MiCuentaResponse resumen(Principal principal) {
        return miCuentaService.resumen(principal.getName());
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse reservar(Principal principal, @Valid @RequestBody MiCuentaReservaRequest request) {
        return miCuentaService.reservar(principal.getName(), request.getIdLibro());
    }

    @PutMapping("/reservas/{idReserva}/cancelar")
    public ReservaResponse cancelarReserva(Principal principal, @PathVariable Integer idReserva) {
        return miCuentaService.cancelarReserva(principal.getName(), idReserva);
    }

    @PutMapping("/prestamos/{idPrestamo}/renovar")
    public PrestamoResponse renovarPrestamo(Principal principal, @PathVariable Integer idPrestamo) {
        return miCuentaService.renovarPrestamo(principal.getName(), idPrestamo);
    }
}
