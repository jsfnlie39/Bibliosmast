package com.biblosmast.service;

import com.biblosmast.model.dto.UsuarioRequest;
import com.biblosmast.model.dto.UsuarioResponse;
import com.biblosmast.model.entity.Cuenta;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Rol;
import com.biblosmast.model.entity.Usuario;
import com.biblosmast.repository.CuentaRepository;
import com.biblosmast.repository.RolRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(String busqueda) {
        Map<Integer, Cuenta> cuentas = new HashMap<>();
        for (Cuenta c : cuentaRepository.listarConRol()) {
            cuentas.put(c.getUsuario().getIdUsuario(), c);
        }
        String q = (busqueda == null) ? "" : busqueda.trim().toLowerCase(Locale.ROOT);

        return usuarioRepository.findAll().stream()
                .map(u -> aRespuesta(u, cuentas.get(u.getIdUsuario())))
                .filter(r -> q.isEmpty() || contiene(r, q))
                .sorted(Comparator.comparing(UsuarioResponse::getApellidos, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UsuarioResponse::getNombres, String.CASE_INSENSITIVE_ORDER))
                .toList();
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

        if (usuarioRepository.existsByDocumento(documento)) {
            throw conflicto("Ya existe un usuario con ese documento");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw conflicto("Ya existe un usuario con ese correo");
        }
        if (cuentaRepository.existsByUsername(username)) {
            throw conflicto("Ese nombre de usuario ya está en uso");
        }
        Rol rol = rolOFalla(r.getIdRol());

        Usuario usuario = new Usuario();
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

        if (usuarioRepository.existsByDocumentoAndIdUsuarioNot(documento, id)) {
            throw conflicto("Ya existe otro usuario con ese documento");
        }
        if (usuarioRepository.existsByEmailAndIdUsuarioNot(email, id)) {
            throw conflicto("Ya existe otro usuario con ese correo");
        }
        Rol rol = rolOFalla(r.getIdRol());
        boolean activo = !Boolean.FALSE.equals(r.getActivo());

        Cuenta cuenta = cuentaRepository.buscarPorIdUsuario(id).orElse(null);
        if (cuenta == null) {
            // Persona sin cuenta: se le crea una (necesita contraseña)
            if (r.getPassword() == null || r.getPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Este usuario no tiene cuenta: escribe una contraseña para crearla");
            }
            if (cuentaRepository.existsByUsername(username)) {
                throw conflicto("Ese nombre de usuario ya está en uso");
            }
            cuenta = new Cuenta();
            cuenta.setUsuario(usuario);
        } else {
            if (cuentaRepository.existsByUsernameAndIdCuentaNot(username, cuenta.getIdCuenta())) {
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

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Usuario usuario = buscarOFalla(id);
        Cuenta cuenta = cuentaRepository.buscarPorIdUsuario(id).orElse(null);

        if (cuenta != null && ROL_ADMIN.equals(cuenta.getRol().getNombre())
                && Boolean.TRUE.equals(cuenta.getActivo())) {
            exigirOtroAdministrador();
        }
        try {
            if (cuenta != null) {
                cuentaRepository.delete(cuenta);
            }
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw conflicto("No se puede eliminar: el usuario tiene préstamos, reservas o multas asociadas. "
                    + "Cámbialo a estado INACTIVO en su lugar");
        }
    }

    // ---------------- helpers ----------------

    private Usuario buscarOFalla(Integer id) {
        return usuarioRepository.findById(id)
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
                .fechaRegistro(u.getFechaRegistro())
                .build();
    }
}
