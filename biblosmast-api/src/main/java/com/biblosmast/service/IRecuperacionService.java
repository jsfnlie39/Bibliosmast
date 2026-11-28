package com.biblosmast.service;

public interface IRecuperacionService {
    /** Si hay una cuenta con ese correo, le envía un enlace de recuperación. Nunca revela si la cuenta existe. */
    void solicitar(String correo);

    /** Cambia la clave con el código del enlace (de un solo uso y con vencimiento). */
    void restablecer(String token, String claveNueva);
}
