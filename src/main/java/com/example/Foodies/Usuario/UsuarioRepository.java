package com.example.Foodies.Usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.Foodies.Enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    /**
     * Loads the account with a pessimistic write lock until the transaction ends. Bookings and
     * waitlist joins take it before the restaurant lock (always in this order), so two requests of the
     * same client are checked one after the other (D1 across restaurants).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.email = :email")
    Optional<Usuario> bloquearPorEmail(@Param("email") String email);
    Boolean existsByEmail(String email);
    Boolean existsByEmailAndPassword(String email, String password);

    List<Usuario> findByRol(Role rol);
}
