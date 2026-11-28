package com.biblosmast.service;

public interface ICorreoService {
    /** Envía un correo de texto. Si no hay servidor de correo configurado, lo muestra en la consola de la API. */
    void enviar(String para, String asunto, String texto);
}
