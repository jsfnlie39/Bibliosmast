package com.biblosmast.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Responde 403 en JSON cuando el usuario sí inició sesión, pero su rol no tiene permiso. */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    public RestAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excepcion) throws IOException {
        RespuestaJson.escribir(mapper, response, HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción");
    }
}
