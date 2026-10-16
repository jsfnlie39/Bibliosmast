package com.biblosmast.repository;

import com.biblosmast.model.entity.Libro;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibroRepository extends JpaRepository<Libro, Integer> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdLibroNot(String isbn, Integer idLibro);

    @Override
    @EntityGraph(attributePaths = {"categoria", "editorial", "autores"})
    List<Libro> findAll();

    @EntityGraph(attributePaths = {"categoria", "editorial", "autores"})
    @Query("select distinct l from Libro l "
            + "where lower(l.titulo) like lower(concat('%', :q, '%')) "
            + "or l.isbn like concat('%', :q, '%')")
    List<Libro> buscar(@Param("q") String q);
}
