package com.example.Foodies.Usuario;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Restaurant.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"restaurant", "password"})
@EqualsAndHashCode(exclude = {"restaurant", "password"})
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String apellido;

    @NotBlank
    @Email(message = "El email debe ser valido")
    @Column(nullable = false, unique = true)
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

}