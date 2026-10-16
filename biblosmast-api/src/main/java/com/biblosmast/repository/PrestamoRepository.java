package com.biblosmast.repository;

import com.biblosmast.model.entity.Prestamo;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrestamoRepository extends JpaRepository<Prestamo, Integer> {

    @EntityGraph(attributePaths = {"usuario", "bibliotecario", "detalles"})
    @Query("select distinct p from Prestamo p order by p.fechaPrestamo desc")
    List<Prestamo> listarTodos();
}
