package com.biblosmast.service;

import com.biblosmast.model.dto.TipoUsuarioRequest;
import com.biblosmast.model.dto.TipoUsuarioResponse;
import com.biblosmast.model.entity.TipoUsuario;
import com.biblosmast.repository.TipoUsuarioRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TipoUsuarioService implements ITipoUsuarioService {

    /** Tipo que se asigna a quien se registra solo (RegistroService). */
    public static final String TIPO_REGISTRO = "EXTERNO";

    private final TipoUsuarioRepository tipoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TipoUsuarioResponse> listar() {
        return tipoRepository.findAllByOrderByNombreAsc().stream().map(this::aRespuesta).toList();
    }

    @Override
    @Transactional
    public TipoUsuarioResponse crear(TipoUsuarioRequest r) {
        String nombre = normalizar(r.getNombre());
        if (tipoRepository.existsByNombreIgnoreCase(nombre)) {
            throw conflicto("Ya existe un tipo de usuario con ese nombre");
        }
        TipoUsuario tipo = new TipoUsuario();
        aplicar(tipo, r, nombre);
        return aRespuesta(tipoRepository.save(tipo));
    }

    @Override
    @Transactional
    public TipoUsuarioResponse actualizar(Integer id, TipoUsuarioRequest r) {
        TipoUsuario tipo = buscarOFalla(id);
        String nombre = normalizar(r.getNombre());
        if (tipoRepository.existsByNombreIgnoreCaseAndIdTipoUsuarioNot(nombre, id)) {
            throw conflicto("Ya existe otro tipo de usuario con ese nombre");
        }
        if (TIPO_REGISTRO.equals(tipo.getNombre()) && !TIPO_REGISTRO.equals(nombre)) {
            throw conflicto("El tipo " + TIPO_REGISTRO + " lo recibe quien se registra solo: no se puede renombrar");
        }
        aplicar(tipo, r, nombre);
        return aRespuesta(tipoRepository.save(tipo));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        TipoUsuario tipo = buscarOFalla(id);
        if (TIPO_REGISTRO.equals(tipo.getNombre())) {
            throw conflicto("El tipo " + TIPO_REGISTRO + " lo recibe quien se registra solo: no se puede eliminar");
        }
        if (usuarioRepository.countByTipoUsuarioIdTipoUsuario(id) > 0) {
            throw conflicto("No se puede eliminar: hay usuarios con este tipo. Cámbiales el tipo primero");
        }
        tipoRepository.delete(tipo);
    }

    private TipoUsuario buscarOFalla(Integer id) {
        return tipoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de usuario no encontrado"));
    }

    /** Los nombres se guardan en mayúsculas (ESTUDIANTE, DOCENTE...) y sin espacios sobrantes. */
    private String normalizar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
    }

    private void aplicar(TipoUsuario tipo, TipoUsuarioRequest r, String nombre) {
        tipo.setNombre(nombre);
        tipo.setMaxLibros(r.getMaxLibros());
        tipo.setDiasPrestamo(r.getDiasPrestamo());
        tipo.setMaxRenovaciones(r.getMaxRenovaciones());
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private TipoUsuarioResponse aRespuesta(TipoUsuario t) {
        return TipoUsuarioResponse.builder()
                .idTipoUsuario(t.getIdTipoUsuario())
                .nombre(t.getNombre())
                .maxLibros(t.getMaxLibros())
                .diasPrestamo(t.getDiasPrestamo())
                .maxRenovaciones(t.getMaxRenovaciones())
                .cantidadUsuarios(usuarioRepository.countByTipoUsuarioIdTipoUsuario(t.getIdTipoUsuario()))
                .build();
    }
}
