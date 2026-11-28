package com.biblosmast.service;

import com.biblosmast.model.dto.MultaResponse;
import com.biblosmast.model.dto.PrestamoResponse;
import com.biblosmast.model.dto.ReservaResponse;
import java.util.List;

/** «Mi cuenta»: todo se filtra por la persona del token; nunca se recibe un idUsuario de afuera. */
public interface IMiCuentaService {

    List<PrestamoResponse> prestamos(String username);

    List<ReservaResponse> reservas(String username);

    List<MultaResponse> multas(String username);

    /** Reserva un libro para la propia persona (entra a la cola igual que una reserva del personal). */
    ReservaResponse reservar(String username, Integer idLibro);

    ReservaResponse cancelarReserva(String username, Integer idReserva);
}
