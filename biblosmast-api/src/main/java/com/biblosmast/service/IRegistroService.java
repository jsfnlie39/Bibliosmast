package com.biblosmast.service;

import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.dto.RegistroRequest;

public interface IRegistroService {
    /** Crea la persona y su cuenta con rol USUARIO y devuelve la sesión ya iniciada. */
    LoginResponse registrar(RegistroRequest request);
}
