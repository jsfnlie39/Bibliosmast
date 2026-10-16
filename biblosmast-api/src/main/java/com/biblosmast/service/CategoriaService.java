package com.biblosmast.service;

import com.biblosmast.model.dto.CategoriaRequest;
import com.biblosmast.model.dto.CategoriaResponse;
import com.biblosmast.model.entity.Categoria;
import com.biblosmast.repository.CategoriaRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CategoriaService implements ICategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream()
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::aRespuesta)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Integer id) {
        return aRespuesta(buscarOFalla(id));
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaRequest r) {
        String nombre = r.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
        Categoria categoria = new Categoria();
        aplicar(categoria, r, nombre);
        return aRespuesta(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Integer id, CategoriaRequest r) {
        Categoria categoria = buscarOFalla(id);
        String nombre = r.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra categoría con ese nombre");
        }
        aplicar(categoria, r, nombre);
        return aRespuesta(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Categoria categoria = buscarOFalla(id);
        try {
            categoriaRepository.delete(categoria);
            categoriaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la categoría tiene libros asociados");
        }
    }

    private Categoria buscarOFalla(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private void aplicar(Categoria c, CategoriaRequest r, String nombre) {
        c.setNombre(nombre);
        c.setDescripcion(r.getDescripcion() == null || r.getDescripcion().isBlank() ? null : r.getDescripcion().trim());
    }

    private CategoriaResponse aRespuesta(Categoria c) {
        return new CategoriaResponse(c.getIdCategoria(), c.getNombre(), c.getDescripcion());
    }
}
