package com.biblosmast.service;

import com.biblosmast.model.dto.EjemplarRequest;
import com.biblosmast.model.dto.EjemplarResponse;
import com.biblosmast.model.entity.Ejemplar;
import com.biblosmast.model.entity.EstadoEjemplar;
import com.biblosmast.model.entity.Libro;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.LibroRepository;
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
public class EjemplarService implements IEjemplarService {

    private final EjemplarRepository ejemplarRepository;
    private final LibroRepository libroRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EjemplarResponse> listar(Integer idLibro) {
        List<Ejemplar> ejemplares = (idLibro == null)
                ? ejemplarRepository.findAll()
                : ejemplarRepository.findByLibroIdLibro(idLibro);
        return ejemplares.stream()
                .map(this::aRespuesta)
                .sorted(Comparator.comparing(EjemplarResponse::getCodigo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EjemplarResponse obtener(Integer id) {
        return aRespuesta(buscarOFalla(id));
    }

    @Override
    @Transactional
    public EjemplarResponse crear(EjemplarRequest r) {
        String codigo = r.getCodigo().trim();
        if (ejemplarRepository.existsByCodigo(codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un ejemplar con ese código");
        }
        validarEstadoAsignable(r.getEstado());

        Ejemplar ejemplar = new Ejemplar();
        ejemplar.setLibro(libroOFalla(r.getIdLibro()));
        ejemplar.setCodigo(codigo);
        ejemplar.setUbicacion(vacioANull(r.getUbicacion()));
        ejemplar.setEstado(r.getEstado() == null ? EstadoEjemplar.DISPONIBLE : r.getEstado());
        return aRespuesta(ejemplarRepository.save(ejemplar));
    }

    @Override
    @Transactional
    public EjemplarResponse actualizar(Integer id, EjemplarRequest r) {
        Ejemplar ejemplar = buscarOFalla(id);
        if (ejemplar.getEstado() == EstadoEjemplar.PRESTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El ejemplar está prestado: edítalo cuando lo devuelvan");
        }
        String codigo = r.getCodigo().trim();
        if (ejemplarRepository.existsByCodigoAndIdEjemplarNot(codigo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro ejemplar con ese código");
        }
        validarEstadoAsignable(r.getEstado());

        ejemplar.setLibro(libroOFalla(r.getIdLibro()));
        ejemplar.setCodigo(codigo);
        ejemplar.setUbicacion(vacioANull(r.getUbicacion()));
        if (r.getEstado() != null) {
            ejemplar.setEstado(r.getEstado());
        }
        return aRespuesta(ejemplarRepository.save(ejemplar));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Ejemplar ejemplar = buscarOFalla(id);
        if (ejemplar.getEstado() == EstadoEjemplar.PRESTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un ejemplar prestado");
        }
        try {
            ejemplarRepository.delete(ejemplar);
            ejemplarRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el ejemplar tiene préstamos en su historial. "
                            + "Cámbialo a estado PERDIDO o DANADO si ya no se usa");
        }
    }

    // ---------------- helpers ----------------

    private Ejemplar buscarOFalla(Integer id) {
        return ejemplarRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ejemplar no encontrado"));
    }

    private Libro libroOFalla(Integer idLibro) {
        return libroRepository.findById(idLibro)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El libro no existe"));
    }

    private void validarEstadoAsignable(EstadoEjemplar estado) {
        if (estado == EstadoEjemplar.PRESTADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estado PRESTADO se asigna al registrar un préstamo, no a mano");
        }
    }

    private String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private EjemplarResponse aRespuesta(Ejemplar e) {
        return EjemplarResponse.builder()
                .idEjemplar(e.getIdEjemplar())
                .idLibro(e.getLibro().getIdLibro())
                .titulo(e.getLibro().getTitulo())
                .isbn(e.getLibro().getIsbn())
                .codigo(e.getCodigo())
                .ubicacion(e.getUbicacion())
                .estado(e.getEstado().name())
                .fechaIngreso(e.getFechaIngreso())
                .build();
    }
}
