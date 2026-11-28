package com.biblosmast.service;

import com.biblosmast.model.dto.ClaveTemporalResponse;
import com.biblosmast.model.dto.UsuarioRequest;
import com.biblosmast.model.dto.UsuarioResponse;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.EstadoMulta;
import com.biblosmast.model.entity.EstadoPrestamo;
import com.biblosmast.model.entity.EstadoReserva;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Reserva;
import com.biblosmast.model.entity.Rol;
import com.biblosmast.model.entity.TipoUsuario;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.MultaRepository;
import com.biblosmast.repository.PrestamoRepository;
import com.biblosmast.repository.ReservaRepository;
import com.biblosmast.repository.TipoUsuarioRepository;
import com.biblosmast.repository.RolRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private static final String ROL_ADMIN = "ADMIN";

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final IAuditoriaService auditoriaService;
    private final PrestamoRepository prestamoRepository;
    private final MultaRepository multaRepository;
    private final ReservaRepository reservaRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final IColaReservasService colaReservas;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(String busqueda, boolean soloDadosDeBaja) {
        Map<Integer, Cuenta> cuentas = new HashMap<>();
        for (Cuenta c : cuentaRepository.listarConRol()) {
            cuentas.put(c.getUsuario().getIdUsuario(), c);
        }
        String q = (busqueda == null) ? "" : busqueda.trim().toLowerCase(Locale.ROOT);

        List<Usuario> usuarios = soloDadosDeBaja ? usuarioRepository.findByEliminadoTrue() : usuarioRepository.findByEliminadoFalse();
        return usuarios.stream()
                .map(u -> aRespuesta(u, cuentas.get(u.getIdUsuario())))
                .filter(r -> q.isEmpty() || contiene(r, q))
                .sorted(Comparator.comparing(UsuarioResponse::getApellidos, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UsuarioResponse::getNombres, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerConBajas(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return aRespuesta(usuario, cuentaRepository.buscarPorIdUsuario(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Integer id) {
        Usuario usuario = buscarOFalla(id);
        return aRespuesta(usuario, cuentaRepository.buscarPorIdUsuario(id).orElse(null));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest r) {
        if (r.getPassword() == null || r.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria al crear un usuario");
        }
        String documento = r.getDocumento().trim();
        String email = r.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = r.getUsername().trim();

        if (usuarioRepository.existsByDocumentoAndEliminadoFalse(documento)) {
            throw conflicto("Ya existe un usuario con ese documento");
        }
        if (usuarioRepository.existsByEmailAndEliminadoFalse(email)) {
            throw conflicto("Ya existe un usuario con ese correo");
        }
        if (cuentaRepository.existsByUsernameAndEliminadaFalse(username)) {
            throw conflicto("Ese nombre de usuario ya está en uso");
        }
        Rol rol = rolOFalla(r.getIdRol());
        TipoUsuario tipo = tipoOFalla(r.getIdTipoUsuario());

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(tipo);
        aplicarDatosPersonales(usuario, r, documento, email);
        usuario = usuarioRepository.save(usuario);

        Cuenta cuenta = new Cuenta();
        cuenta.setUsuario(usuario);
        cuenta.setUsername(username);
        cuenta.setPasswordHash(passwordEncoder.encode(r.getPassword()));
        cuenta.setRol(rol);
        cuenta.setActivo(!Boolean.FALSE.equals(r.getActivo()));
        cuenta = cuentaRepository.save(cuenta);

        return aRespuesta(usuario, cuenta);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Integer id, UsuarioRequest r) {
        Usuario usuario = buscarOFalla(id);
        String documento = r.getDocumento().trim();
        String email = r.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = r.getUsername().trim();

        if (usuarioRepository.existsByDocumentoAndIdUsuarioNotAndEliminadoFalse(documento, id)) {
            throw conflicto("Ya existe otro usuario con ese documento");
        }
        if (usuarioRepository.existsByEmailAndIdUsuarioNotAndEliminadoFalse(email, id)) {
            throw conflicto("Ya existe otro usuario con ese correo");
        }
        Rol rol = rolOFalla(r.getIdRol());
        TipoUsuario tipo = tipoOFalla(r.getIdTipoUsuario());
        boolean activo = !Boolean.FALSE.equals(r.getActivo());

        Cuenta cuenta = cuentaRepository.buscarPorIdUsuario(id).orElse(null);
        if (cuenta == null) {
            // Persona sin cuenta: se le crea una (necesita contraseña)
            if (r.getPassword() == null || r.getPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Este usuario no tiene cuenta: escribe una contraseña para crearla");
            }
            if (cuentaRepository.existsByUsernameAndEliminadaFalse(username)) {
                throw conflicto("Ese nombre de usuario ya está en uso");
            }
            cuenta = new Cuenta();
            cuenta.setUsuario(usuario);
        } else {
            if (cuentaRepository.existsByUsernameAndIdCuentaNotAndEliminadaFalse(username, cuenta.getIdCuenta())) {
                throw conflicto("Ese nombre de usuario ya está en uso");
            }
            boolean eraAdminActivo = ROL_ADMIN.equals(cuenta.getRol().getNombre())
                    && Boolean.TRUE.equals(cuenta.getActivo());
            boolean seguiraAdminActivo = ROL_ADMIN.equals(rol.getNombre()) && activo
                    && r.getEstado() != EstadoUsuario.INACTIVO;
            if (eraAdminActivo && !seguiraAdminActivo) {
                exigirOtroAdministrador();
            }
        }

        usuario.setTipoUsuario(tipo);
        aplicarDatosPersonales(usuario, r, documento, email);
        usuarioRepository.save(usuario);

        cuenta.setUsername(username);
        cuenta.setRol(rol);
        cuenta.setActivo(activo);
        if (r.getPassword() != null && !r.getPassword().isBlank()) {
            cuenta.setPasswordHash(passwordEncoder.encode(r.getPassword()));
        }
        cuenta = cuentaRepository.save(cuenta);

        return aRespuesta(usuario, cuenta);
    }

    /**
     * Baja lógica: el usuario NO se borra. Queda marcado como eliminado, su cuenta se bloquea y se cierran sus sesiones,
     * pero se conservan sus datos y todo su historial (préstamos, reservas, multas y auditoría).
     * Su documento, su correo y su nombre de usuario quedan libres para un registro nuevo.
     */
    @Override
    @Transactional
    public void eliminar(Integer id) {
        Usuario usuario = buscarOFalla(id);
        Cuenta cuenta = cuentaRepository.buscarPorIdUsuario(id).orElse(null);

        if (cuenta != null && ROL_ADMIN.equals(cuenta.getRol().getNombre())
                && Boolean.TRUE.equals(cuenta.getActivo())) {
            exigirOtroAdministrador();
        }
        if (prestamoRepository.existsByUsuarioIdUsuarioAndEstado(id, EstadoPrestamo.ACTIVO)) {
            throw conflicto("No se puede dar de baja: el usuario tiene préstamos sin devolver");
        }
        if (multaRepository.existsByUsuarioIdUsuarioAndEstado(id, EstadoMulta.PENDIENTE)) {
            throw conflicto("No se puede dar de baja: el usuario tiene multas pendientes de pago");
        }

        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        usuario.setEliminado(true);
        usuario.setFechaBaja(ahora);
        usuario.setEstado(EstadoUsuario.INACTIVO); // así tampoco puede recibir préstamos ni reservas
        usuarioRepository.save(usuario);

        if (cuenta != null) {
            cuenta.setEliminada(true);
            cuenta.setActivo(false);
            cuenta.setTokenDesde(ahora); // cierra sus sesiones abiertas
            cuentaRepository.save(cuenta);
        }

        cancelarReservasPendientes(usuario);
    }

    /**
     * Un usuario dado de baja no puede recoger sus reservas: se cancelan las pendientes y las listas, y queda constancia.
     * El ejemplar que tenía apartado una reserva LISTA pasa al siguiente de la cola.
     */
    private void cancelarReservasPendientes(Usuario usuario) {
        List<Reserva> pendientes = new ArrayList<>(reservaRepository
                .findByUsuarioIdUsuarioAndEstado(usuario.getIdUsuario(), EstadoReserva.PENDIENTE));
        List<Reserva> listas = reservaRepository
                .findByUsuarioIdUsuarioAndEstado(usuario.getIdUsuario(), EstadoReserva.LISTA);
        if (pendientes.isEmpty() && listas.isEmpty()) {
            return;
        }
        for (Reserva reserva : pendientes) {
            reserva.setEstado(EstadoReserva.CANCELADA);
        }
        reservaRepository.saveAll(pendientes);
        for (Reserva reserva : listas) {
            colaReservas.cerrar(reserva, EstadoReserva.CANCELADA);
        }
        pendientes.addAll(listas); // solo para contarlas en la auditoría
        try {
            Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
            String actor = (autenticacion == null) ? null : autenticacion.getName();
            auditoriaService.registrarDe(actor, "CANCELAR", "reserva", null,
                    pendientes.size() + " reserva(s) pendiente(s) canceladas por la baja del usuario N° " + usuario.getIdUsuario());
        } catch (Exception e) {
            LoggerFactory.getLogger(UsuarioService.class).warn("No se pudo registrar la auditoría: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public ClaveTemporalResponse restablecerClave(Integer id, String actor) {
        buscarOFalla(id);
        Cuenta cuenta = cuentaRepository.buscarPorIdUsuario(id)
                .orElseThrow(() -> conflicto("Este usuario no tiene cuenta de acceso"));

        String temporal = generarClaveTemporal();
        cuenta.setPasswordHash(passwordEncoder.encode(temporal));
        cuenta.setDebeCambiarClave(true);
        cuenta.setIntentosFallidos(0);
        cuenta.setBloqueadoHasta(null);
        cuenta.setTokenDesde(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)); // cierra sus sesiones abiertas
        cuentaRepository.save(cuenta);

        try {
            // La clave temporal nunca se guarda en la auditoría
            auditoriaService.registrarDe(actor, "CLAVE_RESTABLECIDA", "cuenta", cuenta.getIdCuenta(),
                    "Contraseña temporal para '" + cuenta.getUsername() + "'");
        } catch (Exception e) {
            LoggerFactory.getLogger(UsuarioService.class).warn("No se pudo registrar la auditoría: {}", e.getMessage());
        }
        return new ClaveTemporalResponse(cuenta.getUsername(), temporal);
    }

    /** 10 caracteres sin los que se confunden (0, O, 1, l, I). */
    private String generarClaveTemporal() {
        final String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom azar = new SecureRandom();
        StringBuilder clave = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            clave.append(alfabeto.charAt(azar.nextInt(alfabeto.length())));
        }
        return clave.toString();
    }

    // ---------------- helpers ----------------

    private Usuario buscarOFalla(Integer id) {
        return usuarioRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getEliminado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Rol rolOFalla(Integer idRol) {
        return rolRepository.findById(idRol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol no existe"));
    }

    private void exigirOtroAdministrador() {
        if (cuentaRepository.contarAdministradoresActivos() <= 1) {
            throw conflicto("Debe existir al menos un administrador activo");
        }
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private void aplicarDatosPersonales(Usuario u, UsuarioRequest r, String documento, String email) {
        u.setDocumento(documento);
        u.setNombres(r.getNombres().trim());
        u.setApellidos(r.getApellidos().trim());
        u.setEmail(email);
        u.setTelefono(r.getTelefono() == null || r.getTelefono().isBlank() ? null : r.getTelefono().trim());
        u.setEstado(r.getEstado());
    }

    private boolean contiene(UsuarioResponse r, String q) {
        return (r.getNombres() + " " + r.getApellidos() + " " + r.getDocumento() + " "
                + r.getEmail() + " " + (r.getUsername() == null ? "" : r.getUsername()))
                .toLowerCase(Locale.ROOT).contains(q);
    }

    private TipoUsuario tipoOFalla(Integer idTipoUsuario) {
        return tipoUsuarioRepository.findById(idTipoUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de usuario no existe"));
    }

    private UsuarioResponse aRespuesta(Usuario u, Cuenta c) {
        return UsuarioResponse.builder()
                .idUsuario(u.getIdUsuario())
                .documento(u.getDocumento())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .email(u.getEmail())
                .telefono(u.getTelefono())
                .estado(u.getEstado().name())
                .username(c == null ? null : c.getUsername())
                .idRol(c == null ? null : c.getRol().getIdRol())
                .rol(c == null ? null : c.getRol().getNombre())
                .activo(c == null ? null : c.getActivo())
                .idTipoUsuario(u.getTipoUsuario().getIdTipoUsuario())
                .tipoUsuario(u.getTipoUsuario().getNombre())
                .fechaRegistro(u.getFechaRegistro())
                .eliminado(Boolean.TRUE.equals(u.getEliminado()))
                .fechaBaja(u.getFechaBaja())
                .build();
    }
}
