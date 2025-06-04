package com.example.Foodies.Carta;

import com.example.Foodies.Carta.Dtos.CartaDetailDTO;
import com.example.Foodies.Carta.Dtos.CartaRequestDTO;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private CartaMapper cartaMapper;

    public void guardarCarta(MultipartFile archivo, Long restaurantId) {
        if (archivo.isEmpty() || !archivo.getContentType().equals("application/pdf")) {
            throw new BusinessException("Debe subir un archivo PDF válido");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        // Verificamos si ya tiene una carta asociada
        if (cartaRepository.existByRestaurantId(restaurantId)) {
            throw new BusinessException("Este restaurante ya tiene una carta");
        }

        try {
            Carta carta = new Carta();
            carta.setNombreArchivo(archivo.getOriginalFilename());
            carta.setContenidoPdf(archivo.getBytes());
            carta.setRestaurant(restaurant);

            cartaRepository.save(carta);
        } catch (IOException e) {
            throw new BusinessException("Error al procesar el archivo PDF");
        }
    }

    public CartaDetailDTO getCartaByRestaurantId(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        Carta carta = cartaRepository.findByRestaurant(restaurant)
                .orElseThrow(() -> new EntityNotFoundException("Carta no encontrada"));

        return cartaMapper.toDTO(carta);
    }

    public byte[] descargarCarta(Long cartaId) {
        Carta carta = cartaRepository.findById(cartaId)
                .orElseThrow(() -> new EntityNotFoundException("Carta no encontrada"));
        return carta.getContenidoPdf();
    }


}
