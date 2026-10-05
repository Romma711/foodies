package com.example.Foodies.Carta;

import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class CartaService {


    @Autowired
    private CartaRepository cartaRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public void guardarCarta(MultipartFile archivo, Long restaurantId) {
        byte[] contenido = leerPdf(archivo, "Debe subir un archivo PDF válido");

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        verificarAccesoRestaurante(restaurant);

        // Verificamos si ya tiene una carta asociada
        if (cartaRepository.existsByRestaurantId(restaurantId)) {
            throw new BusinessException("Este restaurante ya tiene una carta");
        }

        Carta carta = new Carta();
        carta.setNombreArchivo(archivo.getOriginalFilename());
        carta.setContenidoPdf(contenido);
        carta.setRestaurant(restaurant);

        cartaRepository.save(carta);
    }

    public Carta descargarCarta(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        return cartaRepository.findByRestaurant(restaurant)
                .orElseThrow(() -> new EntityNotFoundException("Carta no encontrada"));
    }

    public void actualizarCarta(MultipartFile nuevoArchivo, Long restaurantId) {
        byte[] contenido = leerPdf(nuevoArchivo, "Solo se permiten archivos PDF");

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        verificarAccesoRestaurante(restaurant);

        Carta carta = cartaRepository.findByRestaurant(restaurant)
                .orElseThrow(() -> new EntityNotFoundException("La carta para este restaurante no existe"));

        carta.setNombreArchivo(nuevoArchivo.getOriginalFilename());
        carta.setContenidoPdf(contenido);
        cartaRepository.save(carta);
    }

    @Transactional
    public void eliminarCarta(Long cartaId) {
        Carta carta = cartaRepository.findById(cartaId)
                .orElseThrow(() -> new EntityNotFoundException("Carta no encontrada"));

        verificarAccesoRestaurante(carta.getRestaurant());

        cartaRepository.delete(carta);
    }

    /**
     * Valida que el archivo sea un PDF de verdad y devuelve su contenido.
     * Mira el Content-Type (getContentType() puede venir null si el request no
     * manda Content-Type en la parte) y ademas la firma del archivo "%PDF-",
     * asi un .txt renombrado no entra. Se lee una sola vez para no consumir el stream.
     */
    private byte[] leerPdf(MultipartFile archivo, String mensajeError) {
        if (archivo == null || archivo.isEmpty() || !"application/pdf".equals(archivo.getContentType())) {
            throw new BusinessException(mensajeError);
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException(mensajeError);
        }

        if (contenido.length < 5
                || contenido[0] != '%' || contenido[1] != 'P'
                || contenido[2] != 'D' || contenido[3] != 'F' || contenido[4] != '-') {
            throw new BusinessException(mensajeError);
        }

        return contenido;
    }

    private void verificarAccesoRestaurante(Restaurant restaurant) {
        Usuario usuarioActual = usuarioActual();
        if (isAdmin(usuarioActual)) {
            return;
        }
        if (restaurant == null || restaurant.getUsuario() == null
                || !restaurant.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés operar sobre este restaurante");
        }
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String email) || email.isBlank()) {
            throw new AccessDeniedException("No autenticado");
        }
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario.getRol() == Role.ROLE_ADMIN;
    }


}
