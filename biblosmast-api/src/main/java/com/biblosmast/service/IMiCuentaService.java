package com.biblosmast.service;

import com.biblosmast.model.dto.MiCuentaResponse;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.dto.ReservaResponse;

/** «Mi cuenta»: la persona sale siempre del token (username), nunca de lo que mande el navegador. */
public interface IMiCuentaService {
    MiCuentaResponse resumen(String username);
    /** Reserva un libro para quien tiene la sesión (las reglas de la cola son las mismas de siempre). */
    ReservaResponse reservar(String username, Integer idLibro);
    /** 404 si la reserva no es suya. */
    ReservaResponse cancelarReserva(String username, Integer idReserva);
    /** 404 si el préstamo no es suyo; las reglas de renovación son las mismas de siempre. */
    PrestamoResponse renovarPrestamo(String username, Integer idPrestamo);
}
