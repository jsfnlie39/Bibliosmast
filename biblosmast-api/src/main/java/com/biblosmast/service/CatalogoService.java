package com.biblosmast.service;

import com.biblosmast.model.dto.OpcionDto;
import com.biblosmast.repository.AutorRepository;
import com.biblosmast.repository.CategoriaRepository;
import com.biblosmast.model.entity.Ejemplar;
import com.biblosmast.model.entity.EstadoEjemplar;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Reserva;
import com.biblosmast.repository.EditorialRepository;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.ReservaRepository;
import com.biblosmast.repository.RolRepository;
import com.biblosmast.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listas de apoyo (solo lectura) para los formularios del frontend. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final EditorialRepository editorialRepository;
    private final AutorRepository autorRepository;
    private final UsuarioRepository usuarioRepository;
    private final EjemplarRepository ejemplarRepository;
    private final ReservaRepository reservaRepository;
    private final RolRepository rolRepository;

    public List<OpcionDto> categorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new OpcionDto(c.getIdCategoria(), c.getNombre()))
                .sorted(Comparator.comparing(OpcionDto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<OpcionDto> editoriales() {
        return editorialRepository.findAll().stream()
                .map(e -> new OpcionDto(e.getIdEditorial(), e.getNombre()))
                .sorted(Comparator.comparing(OpcionDto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<OpcionDto> autores() {
        return autorRepository.findAll().stream()
                .map(a -> new OpcionDto(a.getIdAutor(), a.nombreCompleto()))
                .sorted(Comparator.comparing(OpcionDto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<OpcionDto> usuariosActivos() {
        return usuarioRepository.findByEstadoAndEliminadoFalse(EstadoUsuario.ACTIVO).stream()
                .map(u -> new OpcionDto(u.getIdUsuario(),
                        u.getNombres() + " " + u.getApellidos() + " (" + u.getDocumento() + ") · "
                                + u.getTipoUsuario().getNombre()))
                .sorted(Comparator.comparing(OpcionDto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Ejemplares que se pueden prestar. Si se indica el usuario, se suman los que tiene apartados
     * por una reserva lista para recoger.
     */
    public List<OpcionDto> ejemplaresDisponibles(Integer idUsuario) {
        List<OpcionDto> opciones = new ArrayList<>(ejemplarRepository.findByEstado(EstadoEjemplar.DISPONIBLE).stream()
                .map(e -> new OpcionDto(e.getIdEjemplar(), e.getCodigo() + " · " + e.getLibro().getTitulo()))
                .toList());
        if (idUsuario != null) {
            for (Reserva reserva : reservaRepository.listasDeUsuario(idUsuario)) {
                Ejemplar e = reserva.getEjemplar();
                opciones.add(new OpcionDto(e.getIdEjemplar(),
                        e.getCodigo() + " · " + e.getLibro().getTitulo() + " (apartado para este usuario)"));
            }
        }
        return opciones.stream()
                .sorted(Comparator.comparing(OpcionDto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<OpcionDto> roles() {
        return rolRepository.findAll().stream()
                .map(r -> new OpcionDto(r.getIdRol(), r.getNombre()))
                .sorted(Comparator.comparing(OpcionDto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
