package com.biblosmast.service;

import com.biblosmast.model.dto.CambioClaveRequest;
import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.dto.PerfilRequest;
import com.biblosmast.model.dto.PerfilResponse;

/** El usuario siempre sale del token, nunca de un parámetro: cada persona solo puede tocar su propio perfil. */
public interface IPerfilService {
    PerfilResponse obtener(String username);
    PerfilResponse actualizar(String username, PerfilRequest request);
    /** Cambia la clave, cierra las demás sesiones y devuelve una sesión nueva. */
    LoginResponse cambiarClave(String username, CambioClaveRequest request);
}
