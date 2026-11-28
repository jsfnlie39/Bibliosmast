package com.biblosmast.service;

import com.biblosmast.model.dto.MultaResponse;
import java.util.List;

public interface IMultaService {
    /** estado: PENDIENTE o PAGADA (null o vacío = todas). idUsuario opcional. */
    List<MultaResponse> listar(String estado, Integer idUsuario);
    MultaResponse obtener(Integer id);
    MultaResponse pagar(Integer id);
    /** Anula (elimina) una multa pendiente. */
    void anular(Integer id);
}
