package com.biblosmast.repository;

import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    List<Usuario> findByEstado(EstadoUsuario estado);

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdUsuarioNot(String documento, Integer idUsuario);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdUsuarioNot(String email, Integer idUsuario);
}
