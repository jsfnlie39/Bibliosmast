package com.biblosmast.service;

import com.biblosmast.model.dto.AuditoriaResponse;
import java.time.LocalDate;
import java.util.List;

public interface IAuditoriaService {

    /** Guarda un registro en su propia transacción (no se pierde si la operación original falla). */
    void registrar(Integer idUsuario, String accion, String tabla, Integer idRegistro, String detalle);

    /** Igual que registrar, pero a partir del nombre de usuario de la cuenta (el que viene en el token). */
    void registrarDe(String username, String accion, String tabla, Integer idRegistro, String detalle);

    /** Id de la persona dueña de esa cuenta, o null si no existe. */
    Integer idUsuarioDe(String username);

    /** Todos los filtros son opcionales. Por defecto: últimos 7 días y máximo 200 registros. */
    List<AuditoriaResponse> listar(String accion, Integer idUsuario, LocalDate desde, LocalDate hasta, Integer limite);
}
