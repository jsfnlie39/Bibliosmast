package com.biblosmast.service;

import com.biblosmast.model.dto.AutorRequest;
import com.biblosmast.model.dto.AutorResponse;
import com.biblosmast.model.entity.Autor;
import com.biblosmast.repository.AutorRepository;
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
public class AutorService implements IAutorService {

    private final AutorRepository autorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AutorResponse> listar() {
        return autorRepository.findAll().stream()
                .sorted(Comparator.comparing(Autor::getApellidos, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Autor::getNombres, String.CASE_INSENSITIVE_ORDER))
                .map(this::aRespuesta)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AutorResponse obtener(Integer id) {
        return aRespuesta(buscarOFalla(id));
    }

    @Override
    @Transactional
    public AutorResponse crear(AutorRequest r) {
        Autor autor = new Autor();
        aplicar(autor, r);
        return aRespuesta(autorRepository.save(autor));
    }

    @Override
    @Transactional
    public AutorResponse actualizar(Integer id, AutorRequest r) {
        Autor autor = buscarOFalla(id);
        aplicar(autor, r);
        return aRespuesta(autorRepository.save(autor));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Autor autor = buscarOFalla(id);
        try {
            autorRepository.delete(autor);
            autorRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el autor tiene libros asociados");
        }
    }

    private Autor buscarOFalla(Integer id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autor no encontrado"));
    }

    private void aplicar(Autor a, AutorRequest r) {
        a.setNombres(r.getNombres().trim());
        a.setApellidos(r.getApellidos().trim());
        a.setNacionalidad(r.getNacionalidad() == null || r.getNacionalidad().isBlank() ? null : r.getNacionalidad().trim());
    }

    private AutorResponse aRespuesta(Autor a) {
        return new AutorResponse(a.getIdAutor(), a.getNombres(), a.getApellidos(), a.getNacionalidad());
    }
}
