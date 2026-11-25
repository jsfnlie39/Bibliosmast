package com.biblosmast.repository;

import com.biblosmast.model.entity.EstadoUsuario;
import com.biblosmast.model.entity.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** "Vigente" = no dado de baja. Un usuario dado de baja libera su documento y su correo para un registro nuevo. */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    List<Usuario> findByEliminadoFalse();

    List<Usuario> findByEliminadoTrue();

    List<Usuario> findByEstadoAndEliminadoFalse(EstadoUsuario estado);

    boolean existsByDocumentoAndEliminadoFalse(String documento);

    boolean existsByDocumentoAndIdUsuarioNotAndEliminadoFalse(String documento, Integer idUsuario);

    boolean existsByEmailAndEliminadoFalse(String email);

    boolean existsByEmailAndIdUsuarioNotAndEliminadoFalse(String email, Integer idUsuario);
}
