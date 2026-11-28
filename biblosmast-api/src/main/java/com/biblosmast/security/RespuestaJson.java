package com.biblosmast.security;

import com.biblosmast.model.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Escribe los errores de seguridad con el mismo formato JSON del resto de la API: { status, mensaje }. */
final class RespuestaJson {

    private RespuestaJson() {
    }

    static void escribir(ObjectMapper mapper, HttpServletResponse response, HttpStatus estado, String mensaje)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), new ApiErrorResponse(estado.value(), mensaje, null));
    }
}
