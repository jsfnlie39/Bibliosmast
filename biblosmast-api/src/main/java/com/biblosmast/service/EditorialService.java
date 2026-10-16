package com.biblosmast.service;

import com.biblosmast.model.dto.EditorialRequest;
import com.biblosmast.model.dto.EditorialResponse;
import com.biblosmast.model.entity.Editorial;
import com.biblosmast.repository.EditorialRepository;
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
public class EditorialService implements IEditorialService {

    private final EditorialRepository editorialRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EditorialResponse> listar() {
        return editorialRepository.findAll().stream()
                .sorted(Comparator.comparing(Editorial::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::aRespuesta)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EditorialResponse obtener(Integer id) {
        return aRespuesta(buscarOFalla(id));
    }

    @Override
    @Transactional
    public EditorialResponse crear(EditorialRequest r) {
        String nombre = r.getNombre().trim();
        if (editorialRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una editorial con ese nombre");
        }
        Editorial editorial = new Editorial();
        aplicar(editorial, r, nombre);
        return aRespuesta(editorialRepository.save(editorial));
    }

    @Override
    @Transactional
    public EditorialResponse actualizar(Integer id, EditorialRequest r) {
        Editorial editorial = buscarOFalla(id);
        String nombre = r.getNombre().trim();
        if (editorialRepository.existsByNombreIgnoreCaseAndIdEditorialNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otra editorial con ese nombre");
        }
        aplicar(editorial, r, nombre);
        return aRespuesta(editorialRepository.save(editorial));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Editorial editorial = buscarOFalla(id);
        try {
            editorialRepository.delete(editorial);
            editorialRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la editorial tiene libros asociados");
        }
    }

    private Editorial buscarOFalla(Integer id) {
        return editorialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Editorial no encontrada"));
    }

    private void aplicar(Editorial e, EditorialRequest r, String nombre) {
        e.setNombre(nombre);
        e.setPais(r.getPais() == null || r.getPais().isBlank() ? null : r.getPais().trim());
    }

    private EditorialResponse aRespuesta(Editorial e) {
        return new EditorialResponse(e.getIdEditorial(), e.getNombre(), e.getPais());
    }
}
