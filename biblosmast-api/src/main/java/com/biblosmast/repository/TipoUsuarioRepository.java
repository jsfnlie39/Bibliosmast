package com.biblosmast.repository;

import com.biblosmast.model.entity.TipoUsuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoUsuarioRepository extends JpaRepository<TipoUsuario, Integer> {

    List<TipoUsuario> findAllByOrderByNombreAsc();

    Optional<TipoUsuario> findByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTipoUsuarioNot(String nombre, Integer idTipoUsuario);
}
