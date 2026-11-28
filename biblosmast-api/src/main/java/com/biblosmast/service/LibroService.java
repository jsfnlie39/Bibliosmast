package com.biblosmast.service;

import com.biblosmast.model.dto.LibroFiltro;
import com.biblosmast.model.dto.LibroRequest;
import com.biblosmast.model.dto.LibroResponse;
import com.biblosmast.model.entity.Autor;
import com.biblosmast.model.entity.Categoria;
import com.biblosmast.model.entity.Editorial;
import com.biblosmast.model.entity.Libro;
import com.biblosmast.repository.AutorRepository;
import com.biblosmast.repository.CategoriaRepository;
import com.biblosmast.repository.EditorialRepository;
import com.biblosmast.repository.EjemplarRepository;
import com.biblosmast.repository.LibroRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LibroService implements ILibroService {

    private final LibroRepository libroRepository;
    private final CategoriaRepository categoriaRepository;
    private final EditorialRepository editorialRepository;
    private final AutorRepository autorRepository;
    private final EjemplarRepository ejemplarRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LibroResponse> listar(String busqueda) {
        return buscar(LibroFiltro.builder().q(busqueda).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibroResponse> buscar(LibroFiltro f) {
        if (f.getAnioDesde() != null && f.getAnioHasta() != null && f.getAnioDesde() > f.getAnioHasta()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El año inicial no puede ser mayor que el final");
        }
        String q = (f.getQ() == null) ? "" : f.getQ().trim();
        String idioma = (f.getIdioma() == null) ? "" : f.getIdioma().trim();

        List<Libro> libros = q.isEmpty() ? libroRepository.findAll() : libroRepository.buscar(q);
        Map<Integer, long[]> conteos = conteoEjemplares();
        return libros.stream()
                .filter(l -> f.getIdCategoria() == null || l.getCategoria().getIdCategoria().equals(f.getIdCategoria()))
                .filter(l -> f.getIdEditorial() == null || l.getEditorial().getIdEditorial().equals(f.getIdEditorial()))
                .filter(l -> f.getIdAutor() == null
                        || l.getAutores().stream().anyMatch(a -> a.getIdAutor().equals(f.getIdAutor())))
                .filter(l -> idioma.isEmpty() || idioma.equalsIgnoreCase(l.getIdioma()))
                // Sin año registrado, el libro no entra cuando se pide un rango
                .filter(l -> f.getAnioDesde() == null
                        || (l.getAnioPublicacion() != null && l.getAnioPublicacion() >= f.getAnioDesde()))
                .filter(l -> f.getAnioHasta() == null
                        || (l.getAnioPublicacion() != null && l.getAnioPublicacion() <= f.getAnioHasta()))
                .map(l -> aRespuesta(l, conteos))
                .filter(l -> !f.isSoloDisponibles() || l.getEjemplaresDisponibles() > 0)
                .sorted(Comparator.comparing(LibroResponse::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LibroResponse obtener(Integer id) {
        return aRespuesta(buscarOFalla(id), conteoEjemplares());
    }

    @Override
    @Transactional
    public LibroResponse crear(LibroRequest request) {
        String isbn = request.getIsbn().trim();
        if (libroRepository.existsByIsbn(isbn)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un libro con ese ISBN");
        }
        Libro libro = new Libro();
        aplicar(libro, request);
        return aRespuesta(libroRepository.save(libro), conteoEjemplares());
    }

    @Override
    @Transactional
    public LibroResponse actualizar(Integer id, LibroRequest request) {
        Libro libro = buscarOFalla(id);
        String isbn = request.getIsbn().trim();
        if (libroRepository.existsByIsbnAndIdLibroNot(isbn, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro libro con ese ISBN");
        }
        aplicar(libro, request);
        return aRespuesta(libroRepository.save(libro), conteoEjemplares());
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Libro libro = buscarOFalla(id);
        libroRepository.delete(libro);
        // flush para que, si tiene ejemplares asociados, el error salga aqui (409) y no al cerrar la transaccion
        libroRepository.flush();
    }

    // ---------------- helpers ----------------

    private Libro buscarOFalla(Integer id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
    }

    private void aplicar(Libro libro, LibroRequest r) {
        Categoria categoria = categoriaRepository.findById(r.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe"));
        Editorial editorial = editorialRepository.findById(r.getIdEditorial())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La editorial no existe"));

        Set<Integer> idsUnicos = new HashSet<>(r.getIdsAutores());
        List<Autor> autores = autorRepository.findAllById(idsUnicos);
        if (autores.size() != idsUnicos.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alguno de los autores no existe");
        }

        libro.setIsbn(r.getIsbn().trim());
        libro.setTitulo(r.getTitulo().trim());
        libro.setCategoria(categoria);
        libro.setEditorial(editorial);
        libro.setAnioPublicacion(r.getAnioPublicacion());
        libro.setEdicion(vacioANull(r.getEdicion()));
        libro.setIdioma(r.getIdioma() == null || r.getIdioma().isBlank() ? "Español" : r.getIdioma().trim());
        libro.setNumeroPaginas(r.getNumeroPaginas());
        libro.setDescripcion(vacioANull(r.getDescripcion()));
        libro.getAutores().clear();
        libro.getAutores().addAll(autores);
    }

    private String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Por libro: [total de ejemplares, ejemplares disponibles]. */
    private Map<Integer, long[]> conteoEjemplares() {
        Map<Integer, long[]> conteos = new HashMap<>();
        for (Object[] fila : ejemplarRepository.contarPorLibro()) {
            conteos.put((Integer) fila[0], new long[]{((Number) fila[1]).longValue(), ((Number) fila[2]).longValue()});
        }
        return conteos;
    }

    private LibroResponse aRespuesta(Libro l, Map<Integer, long[]> conteos) {
        long[] conteo = conteos.getOrDefault(l.getIdLibro(), new long[]{0, 0});
        List<Autor> autores = l.getAutores().stream()
                .sorted(Comparator.comparing(Autor::getApellidos, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return LibroResponse.builder()
                .idLibro(l.getIdLibro())
                .isbn(l.getIsbn())
                .titulo(l.getTitulo())
                .idCategoria(l.getCategoria().getIdCategoria())
                .categoria(l.getCategoria().getNombre())
                .idEditorial(l.getEditorial().getIdEditorial())
                .editorial(l.getEditorial().getNombre())
                .anioPublicacion(l.getAnioPublicacion())
                .edicion(l.getEdicion())
                .idioma(l.getIdioma())
                .numeroPaginas(l.getNumeroPaginas())
                .descripcion(l.getDescripcion())
                .estado(l.getEstado().name())
                .idsAutores(autores.stream().map(Autor::getIdAutor).toList())
                .autores(autores.stream().map(Autor::nombreCompleto).toList())
                .totalEjemplares(conteo[0])
                .ejemplaresDisponibles(conteo[1])
                .build();
    }
}
