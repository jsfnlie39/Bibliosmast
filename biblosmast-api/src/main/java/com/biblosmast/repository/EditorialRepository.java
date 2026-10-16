package com.biblosmast.repository;

import com.biblosmast.model.entity.Editorial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EditorialRepository extends JpaRepository<Editorial, Integer> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdEditorialNot(String nombre, Integer idEditorial);
}
