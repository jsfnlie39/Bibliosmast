package com.biblosmast.config;

import com.biblosmast.service.IAuditoriaService;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Deja constancia de cada cambio que la API acepta (POST, PUT o DELETE con respuesta 2xx).
 * Quién lo hizo sale del token JWT ya validado (SecurityContext), no de lo que diga el navegador.
 * Los inicios de sesión, el cambio y el restablecimiento de clave se auditan aparte, en sus servicios,
 * con acciones más específicas. Si auditar falla, la operación no se afecta.
 */
@Component
@RequiredArgsConstructor
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaInterceptor.class);

    private static final Map<String, String> TABLAS = Map.ofEntries(
            Map.entry("libros", "libro"), Map.entry("ejemplares", "ejemplar"), Map.entry("prestamos", "prestamo"),
            Map.entry("usuarios", "usuario"), Map.entry("autores", "autor"), Map.entry("categorias", "categoria"),
            Map.entry("editoriales", "editorial"), Map.entry("reservas", "reserva"), Map.entry("perfil", "usuario"),
            Map.entry("multas", "multa"), Map.entry("configuracion", "configuracion"));

    private final IAuditoriaService auditoriaService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String metodo = request.getMethod();
        boolean cambia = "POST".equals(metodo) || "PUT".equals(metodo) || "DELETE".equals(metodo);
        int estado = response.getStatus();
        String ruta = request.getRequestURI();
        boolean auditadoAparte = ruta.startsWith("/api/auth/") || ruta.endsWith("/clave") || ruta.endsWith("/clave-temporal");
        if (!cambia || estado < 200 || estado >= 300 || auditadoAparte) {
            return;
        }

        try {
            String[] partes = ruta.split("/");               // ["", "api", "libros", "5", ...]
            // «Mi cuenta» cuelga de /api/mi-cuenta/{recurso}/{id}: se corre un lugar para auditar el recurso de verdad
            int base = (partes.length > 3 && "mi-cuenta".equals(partes[2])) ? 3 : 2;
            String recurso = partes.length > base ? partes[base] : "";
            Integer idRegistro = (partes.length > base + 1 && partes[base + 1].matches("\\d{1,9}")) ? Integer.valueOf(partes[base + 1]) : null;
            String accion = ruta.endsWith("/cancelar") ? "CANCELAR"
                    : ruta.endsWith("/devolucion") ? "DEVOLUCION"
                    : ruta.endsWith("/pago") ? "PAGO"
                    : ruta.endsWith("/renovar") ? "RENOVACION"
                    : "POST".equals(metodo) ? "CREAR"
                    : "PUT".equals(metodo) ? "ACTUALIZAR" : "ELIMINAR";

            auditoriaService.registrarDe(usuarioAutenticado(), accion, TABLAS.getOrDefault(recurso, recurso),
                    idRegistro, metodo + " " + ruta);
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría de {} {}: {}", metodo, ruta, e.getMessage());
        }
    }

    private String usuarioAutenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated() || autenticacion instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return autenticacion.getName();
    }
}
