package com.biblosmast.security;

import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.repository.CuentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Se ejecuta en cada petición: si trae "Authorization: Bearer <token>", comprueba la firma y el vencimiento
 * y además consulta la cuenta en la base de datos:
 *  - si la cuenta ya no existe o está inactiva, el token no sirve;
 *  - si la sesión fue cerrada o se cambió la clave después de emitir el token, tampoco;
 *  - el rol que vale es el actual de la base de datos, no el que quedó escrito en el token;
 *  - con una clave temporal, solo se permite cambiarla o cerrar sesión.
 * Si el token no sirve, la petición sigue sin autenticar y SeguridadWebConfig responde 401.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Atributo de la petición que indica por qué se rechazó el token (lo lee RestAuthenticationEntryPoint). */
    public static final String ATRIBUTO_ERROR = "jwt.error";

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final CuentaRepository cuentaRepository;
    private final ObjectMapper mapper;

    public JwtAuthFilter(JwtService jwtService, CuentaRepository cuentaRepository, ObjectMapper mapper) {
        this.jwtService = jwtService;
        this.cuentaRepository = cuentaRepository;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            try {
                Claims claims = jwtService.validar(token);
                Cuenta cuenta = cuentaRepository.buscarPorUsername(claims.getSubject()).orElse(null);
                String problema = problemaDelToken(cuenta, claims);

                if (problema != null) {
                    request.setAttribute(ATRIBUTO_ERROR, problema);
                } else {
                    UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                            cuenta.getUsername(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + cuenta.getRol().getNombre())));
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);

                    if (Boolean.TRUE.equals(cuenta.getDebeCambiarClave()) && !permitidoConClaveTemporal(request)) {
                        SecurityContextHolder.clearContext();
                        RespuestaJson.escribir(mapper, response, HttpStatus.FORBIDDEN,
                                "Debes cambiar tu contraseña antes de continuar");
                        return;
                    }
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(ATRIBUTO_ERROR, "expirado");
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(ATRIBUTO_ERROR, "invalido");
            }
        }

        cadena.doFilter(request, response);
    }

    /** null si el token sirve; si no, el motivo ("invalido" o "cerrada"). */
    private String problemaDelToken(Cuenta cuenta, Claims claims) {
        if (cuenta == null || !Boolean.TRUE.equals(cuenta.getActivo())
                || cuenta.getUsuario().getEstado() == EstadoUsuario.INACTIVO
                || claims.getIssuedAt() == null) {
            return "invalido";
        }
        if (cuenta.getTokenDesde() != null) {
            long emitido = claims.getIssuedAt().getTime() / 1000;
            long limite = cuenta.getTokenDesde().atZone(ZoneId.systemDefault()).toEpochSecond();
            if (emitido < limite) {
                return "cerrada";
            }
        }
        return null;
    }

    private boolean permitidoConClaveTemporal(HttpServletRequest request) {
        String metodo = request.getMethod();
        String ruta = request.getRequestURI();
        return "OPTIONS".equals(metodo)
                || ("GET".equals(metodo) && ruta.equals("/api/perfil"))
                || ("PUT".equals(metodo) && ruta.equals("/api/perfil/clave"))
                || ("POST".equals(metodo) && ruta.equals("/api/auth/logout"));
    }
}
