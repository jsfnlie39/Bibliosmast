package com.biblosmast.service;

import com.biblosmast.model.dto.LoginRequest;
import com.biblosmast.model.dto.LoginResponse;
import com.biblosmast.model.entity.Cuenta;

public interface IAuthService {
    LoginResponse login(LoginRequest request);

    /** Cierra la sesión: los tokens emitidos hasta ahora dejan de servir. */
    void logout(String username);

    /** Crea un token nuevo para la cuenta y arma la respuesta de sesión (la usan el login y el cambio de clave). */
    LoginResponse emitirSesion(Cuenta cuenta);
}
