package com.biblosmast.security;

import com.biblosmast.repository.CuentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Reglas de seguridad de la API: quién puede entrar a qué ruta. */
@Configuration
@EnableWebSecurity
public class SeguridadWebConfig {

    private static final String ADMIN = "ADMIN";
    private static final String BIBLIOTECARIO = "BIBLIOTECARIO";

    @Value("${app.cors.origins}")
    private String[] origenesPermitidos;

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, JwtService jwtService, CuentaRepository cuentaRepository,
                                       ObjectMapper mapper) throws Exception {
        http
                // CORS lo resuelve Spring Security antes que nada (usa el bean corsConfigurationSource)
                .cors(Customizer.withDefaults())
                // Sin cookies de sesión no hay CSRF: el token viaja en el encabezado Authorization
                .csrf(csrf -> csrf.disable())
                // La API no guarda sesión: cada petición se identifica sola con su token
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(formulario -> formulario.disable())
                .httpBasic(basica -> basica.disable())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(mapper))
                        .accessDeniedHandler(new RestAccessDeniedHandler(mapper)))
                .authorizeRequests(reglas -> reglas
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Públicas: login, registro, recuperación de contraseña y la documentación de la API (Swagger)
                        .antMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/recuperar", "/api/auth/restablecer").permitAll()
                        // /error es donde Spring Boot muestra los errores (404, etc.); sin esto saldría un 401 engañoso
                        .antMatchers("/error").permitAll()
                        .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        // Cada persona ve y edita su propio perfil (la persona sale del token)
                        .antMatchers("/api/perfil/**").authenticated()
                        // «Mi cuenta»: cada persona ve y mueve solo lo suyo (la persona sale del token)
                        .antMatchers("/api/mi-cuenta/**").authenticated()
                        // Auditoría y valores de las multas: solo ADMIN
                        .antMatchers("/api/auditoria/**", "/api/configuracion/**").hasRole(ADMIN)
                        // Cualquier usuario con sesión puede consultar el catálogo de libros
                        .antMatchers(HttpMethod.GET, "/api/libros/**").authenticated()
                        // Lista de usuarios activos para los formularios: personal de la biblioteca
                        .antMatchers(HttpMethod.GET, "/api/usuarios/activos").hasAnyRole(ADMIN, BIBLIOTECARIO)
                        // Administración de usuarios y roles: solo ADMIN
                        .antMatchers("/api/usuarios/**", "/api/roles/**").hasRole(ADMIN)
                        // Eliminar registros es solo de ADMIN: el bibliotecario crea y edita, pero no borra
                        .antMatchers(HttpMethod.DELETE, "/api/libros/**", "/api/ejemplares/**", "/api/reservas/**",
                                "/api/autores/**", "/api/categorias/**", "/api/editoriales/**", "/api/multas/**").hasRole(ADMIN)
                        // El resto de módulos: ADMIN y BIBLIOTECARIO
                        .antMatchers("/api/libros/**", "/api/opciones/**", "/api/ejemplares/**", "/api/prestamos/**",
                                "/api/reservas/**", "/api/autores/**", "/api/categorias/**", "/api/editoriales/**", "/api/multas/**")
                        .hasAnyRole(ADMIN, BIBLIOTECARIO)
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtService, cuentaRepository, mapper), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.asList(origenesPermitidos));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
