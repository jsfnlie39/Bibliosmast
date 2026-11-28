package com.biblosmast.controller;

import com.biblosmast.model.dto.ConteoNotificaciones;
import com.biblosmast.model.dto.NotificacionResponse;
import com.biblosmast.service.INotificacionService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La campana: los avisos de la persona con sesión (sale del token). Cualquier rol. */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final INotificacionService notificacionService;

    @GetMapping
    public List<NotificacionResponse> listar(Principal principal) {
        return notificacionService.listar(principal.getName());
    }

    @GetMapping("/no-leidas")
    public ConteoNotificaciones noLeidas(Principal principal) {
        return new ConteoNotificaciones(notificacionService.contarNoLeidas(principal.getName()));
    }

    @PutMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(Principal principal, @PathVariable Integer id) {
        return notificacionService.marcarLeida(principal.getName(), id);
    }

    @PutMapping("/leer-todas")
    public ConteoNotificaciones leerTodas(Principal principal) {
        return new ConteoNotificaciones(notificacionService.marcarTodasLeidas(principal.getName()));
    }
}
