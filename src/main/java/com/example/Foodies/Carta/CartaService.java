package com.example.Foodies.Carta;

import com.example.Foodies.Carta.Dtos.CartaDTO;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartaService {

    private static final Logger log = LoggerFactory.getLogger(CartaService.class);
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final CartaRepository cartaRepository;
    private final FileStorage storage;

    public CartaService(CartaRepository cartaRepository, FileStorage storage) {
        this.cartaRepository = cartaRepository;
        this.storage = storage;
    }

    /** Creates or replaces the restaurant's menu. The previous file is removed once the new one is committed. */
    @Transactional
    public CartaDTO guardar(Restaurant restaurant, MultipartFile archivo) {
        byte[] contenido = validarPdf(archivo);
        String key = "cartas/%d/%s.pdf".formatted(restaurant.getId(), UUID.randomUUID());
        storage.upload(key, contenido, "application/pdf");

        Carta carta = cartaRepository.findByRestaurant(restaurant).orElseGet(() -> {
            Carta nueva = new Carta();
            nueva.setRestaurant(restaurant);
            return nueva;
        });
        String keyAnterior = carta.getStorageKey();
        carta.setStorageKey(key);
        carta.setNombreArchivo(nombreSeguro(archivo.getOriginalFilename()));
        carta.setTamanioBytes((long) contenido.length);
        carta.setActualizadaEn(Instant.now());

        try {
            carta = cartaRepository.saveAndFlush(carta);
        } catch (RuntimeException e) {
            borrarSinFallar(key);
            throw e;
        }
        if (keyAnterior != null) {
            borrarDespuesDelCommit(keyAnterior);
        }
        return toDto(carta);
    }

    @Transactional
    public void eliminar(Restaurant restaurant) {
        Carta carta = cartaRepository.findByRestaurant(restaurant)
                .orElseThrow(() -> new EntityNotFoundException("El restaurante no tiene carta"));
        eliminar(carta);
    }

    @Transactional
    public void eliminarPorId(Long cartaId) {
        Carta carta = cartaRepository.findById(cartaId)
                .orElseThrow(() -> new EntityNotFoundException("Carta no encontrada"));
        eliminar(carta);
    }

    /** Removes only the stored file; used when the restaurant row (and its carta, by cascade) is deleted. */
    public void eliminarArchivo(Restaurant restaurant) {
        cartaRepository.findByRestaurant(restaurant)
                .map(Carta::getStorageKey)
                .ifPresent(this::borrarDespuesDelCommit);
    }

    public Optional<CartaDTO> buscarPorRestaurante(Restaurant restaurant) {
        return cartaRepository.findByRestaurant(restaurant)
                .filter(carta -> carta.getStorageKey() != null)
                .map(this::toDto);
    }

    public CartaDTO getPorRestauranteId(Long restaurantId) {
        return cartaRepository.findByRestaurantId(restaurantId)
                .filter(carta -> carta.getStorageKey() != null)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Carta no encontrada"));
    }

    public String urlPorId(Long cartaId) {
        return cartaRepository.findById(cartaId)
                .filter(carta -> carta.getStorageKey() != null)
                .map(carta -> storage.publicUrl(carta.getStorageKey()))
                .orElseThrow(() -> new EntityNotFoundException("Carta no encontrada"));
    }

    private void eliminar(Carta carta) {
        String key = carta.getStorageKey();
        carta.getRestaurant().setCarta(null);
        cartaRepository.delete(carta);
        if (key != null) {
            borrarDespuesDelCommit(key);
        }
    }

    private CartaDTO toDto(Carta carta) {
        return new CartaDTO(carta.getId(), carta.getNombreArchivo(), storage.publicUrl(carta.getStorageKey()),
                carta.getTamanioBytes(), carta.getActualizadaEn());
    }

    /** Checks the file content, not the client-supplied content type. */
    private static byte[] validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Debe subir un archivo PDF");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new BusinessException("El archivo supera el tamaño máximo de 10 MB");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Error al leer el archivo PDF");
        }
        if (contenido.length < PDF_SIGNATURE.length
                || !Arrays.equals(Arrays.copyOf(contenido, PDF_SIGNATURE.length), PDF_SIGNATURE)) {
            throw new BusinessException("El archivo no es un PDF válido");
        }
        return contenido;
    }

    private static String nombreSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "carta.pdf";
        }
        String base = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).strip();
        if (base.isEmpty()) {
            return "carta.pdf";
        }
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }

    private void borrarDespuesDelCommit(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    borrarSinFallar(key);
                }
            });
        } else {
            borrarSinFallar(key);
        }
    }

    /** An orphaned file is harmless; failing the request after the database changed would not be. */
    private void borrarSinFallar(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar el archivo {} del almacenamiento", key, e);
        }
    }
}
