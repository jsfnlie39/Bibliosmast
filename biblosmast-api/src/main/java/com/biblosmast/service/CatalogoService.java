package com.biblosmast.service;

import com.biblosmast.model.dto.OpcionDto;
import com.biblosmast.repository.AutorRepository;
import com.biblosmast.repository.CategoriaRepository;
import com.biblosmast.model.entity.EstadoEjemplar;
import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.repository.EditorialRepository;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.RolRepository;
import com.biblosmast.repository.UsuarioRepository;
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
        return usuarioRepository.findByEstado(EstadoUsuario.ACTIVO).stream()
                .map(u -> new OpcionDto(u.getIdUsuario(),
                        u.getNombres() + " " + u.getApellidos() + " (" + u.getDocumento() + ")"))
                .sorted(Comparator.comparing(OpcionDto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<OpcionDto> ejemplaresDisponibles() {
        return ejemplarRepository.findByEstado(EstadoEjemplar.DISPONIBLE).stream()
                .map(e -> new OpcionDto(e.getIdEjemplar(), e.getCodigo() + " · " + e.getLibro().getTitulo()))
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
