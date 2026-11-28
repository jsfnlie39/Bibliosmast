package com.biblosmast.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Responde 401 en JSON cuando falta el token o no sirve (vencido, alterado o mal formado). */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public RestAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excepcion) throws IOException {
        Object motivo = request.getAttribute(JwtAuthFilter.ATRIBUTO_ERROR);
        String mensaje;
        if ("expirado".equals(motivo)) {
            mensaje = "Tu sesión expiró. Inicia sesión de nuevo";
        } else if ("cerrada".equals(motivo)) {
            mensaje = "Tu sesión fue cerrada. Inicia sesión de nuevo";
        } else if ("invalido".equals(motivo)) {
            mensaje = "El token no es válido. Inicia sesión de nuevo";
        } else {
            mensaje = "Debes iniciar sesión para acceder a este recurso";
        }
        RespuestaJson.escribir(mapper, response, HttpStatus.UNAUTHORIZED, mensaje);
    }
}
