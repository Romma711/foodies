package com.example.Foodies.Usuario;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String apellido;

    @NotBlank
    @Email(message = "El email debe ser valido")
    @Column(nullable = false)
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    @Column(nullable = false)
    private String telefono;

    @Enumerated(EnumType.STRING)
    private Role rol;

    @OneToOne(mappedBy = "usuario")
    private Restaurant restaurant;

    /**
     * Fecha y hora de alta. Hibernate la completa al insertar y no se modifica después.
     * Queda en {@code NULL} para los usuarios creados antes de que existiera esta columna.
     */
    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

}