package com.biblosmast.service;

import com.biblosmast.model.dto.NotificacionResponse;
import com.biblosmast.model.entity.TipoNotificacion;
import com.biblosmast.model.entity.Usuario;
import java.util.List;

/** Avisos dentro del sistema (y por correo). Cada persona solo ve y marca los suyos. */
public interface INotificacionService {

    /**
     * Crea el aviso si no existe otro con la misma clave y lo envía también por correo (si hay servidor configurado).
     * Devuelve false si ya existía.
     */
    boolean crearUnica(Usuario usuario, TipoNotificacion tipo, String mensaje, Integer idPrestamo, Integer idReserva,
                       String claveUnica);

    /** Los 30 avisos más recientes de la persona con sesión. */
    List<NotificacionResponse> listar(String username);

    long contarNoLeidas(String username);

    /** 404 si el aviso no existe o es de otra persona. */
    NotificacionResponse marcarLeida(String username, Integer idNotificacion);

    /** Devuelve cuántos avisos marcó. */
    int marcarTodasLeidas(String username);
}
