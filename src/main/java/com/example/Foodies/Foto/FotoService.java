package com.example.Foodies.Foto;

import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Exception.ForbiddenException;
import com.example.Foodies.Foto.Dtos.FotoDTO;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fotos de un restaurante: exactamente 1 del frente (la portada del catálogo) y al menos 2 del salón,
 * entre 3 y 5 en total. Los restaurantes registrados antes de esta regla pueden tener menos y completarlas desde el panel.
 */
@Service
public class FotoService {

    public static final int MIN_FOTOS = 3;
    public static final int MAX_FOTOS = 5;
    public static final int MIN_SALON = 2;
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(FotoService.class);
    private static final Comparator<FotoRestaurante> ORDEN =
            Comparator.comparing(FotoRestaurante::getTipo).thenComparing(FotoRestaurante::getId);

    private final FotoRestauranteRepository fotoRepo;
    private final FileStorage storage;

    public FotoService(FotoRestauranteRepository fotoRepo, FileStorage storage) {
        this.fotoRepo = fotoRepo;
        this.storage = storage;
    }

    /** Valida las fotos del registro antes de crear nada, para no dejar un restaurante a medias. */
    public static void validarRegistro(MultipartFile frente, List<MultipartFile> salones) {
        List<MultipartFile> delSalon = salones == null ? List.of() : salones.stream().filter(f -> !f.isEmpty()).toList();
        if (frente == null || frente.isEmpty()) {
            throw new BusinessException("Subí una foto del frente del restaurante");
        }
        if (delSalon.size() < MIN_SALON) {
            throw new BusinessException("Subí al menos %d fotos del salón".formatted(MIN_SALON));
        }
        if (1 + delSalon.size() > MAX_FOTOS) {
            throw new BusinessException("Podés subir hasta %d fotos en total".formatted(MAX_FOTOS));
        }
        validarImagen(frente);
        delSalon.forEach(FotoService::validarImagen);
    }

    /** Guarda las fotos del registro. Se asume que {@link #validarRegistro} ya pasó. */
    @Transactional
    public void guardarRegistro(Restaurant restaurant, MultipartFile frente, List<MultipartFile> salones) {
        guardarNueva(restaurant, TipoFoto.FRENTE, validarImagen(frente));
        salones.stream().filter(f -> !f.isEmpty())
                .forEach(f -> guardarNueva(restaurant, TipoFoto.SALON, validarImagen(f)));
    }

    @Transactional(readOnly = true)
    public List<FotoDTO> listar(Long restaurantId) {
        return fotoRepo.findAllByRestaurant_Id(restaurantId).stream().sorted(ORDEN).map(this::toDto).toList();
    }

    /** URL de la foto del frente de cada restaurante; los que no tienen foto quedan afuera del mapa. */
    @Transactional(readOnly = true)
    public Map<Long, String> portadas(Collection<Long> restaurantIds) {
        if (restaurantIds.isEmpty()) {
            return Map.of();
        }
        return fotoRepo.findAllByRestaurant_IdInAndTipo(restaurantIds, TipoFoto.FRENTE).stream()
                .collect(Collectors.toMap(f -> f.getRestaurant().getId(), f -> storage.publicUrl(f.getStorageKey()),
                        (a, b) -> a));
    }

    @Transactional
    public FotoDTO agregar(Restaurant restaurant, TipoFoto tipo, MultipartFile archivo) {
        if (tipo == null) {
            throw new BusinessException("Indicá si la foto es del frente o del salón");
        }
        List<FotoRestaurante> actuales = fotoRepo.findAllByRestaurant_Id(restaurant.getId());
        if (actuales.size() >= MAX_FOTOS) {
            throw new BusinessException("Podés tener hasta %d fotos. Borrá una antes de subir otra.".formatted(MAX_FOTOS));
        }
        if (tipo == TipoFoto.FRENTE && actuales.stream().anyMatch(f -> f.getTipo() == TipoFoto.FRENTE)) {
            throw new BusinessException("Ya tenés una foto del frente. Reemplazala en lugar de subir otra.");
        }
        return toDto(guardarNueva(restaurant, tipo, validarImagen(archivo)));
    }

    /** Cambia la imagen manteniendo el tipo. El archivo anterior se borra una vez confirmado el cambio. */
    @Transactional
    public FotoDTO reemplazar(Restaurant restaurant, Long fotoId, MultipartFile archivo) {
        FotoRestaurante foto = propia(restaurant, fotoId);
        Imagen imagen = validarImagen(archivo);
        String keyAnterior = foto.getStorageKey();
        foto.setStorageKey(subir(restaurant, imagen));
        foto.setCreadaEn(Instant.now());
        foto = fotoRepo.saveAndFlush(foto);
        borrarDespuesDelCommit(keyAnterior);
        return toDto(foto);
    }

    @Transactional
    public void eliminar(Restaurant restaurant, Long fotoId) {
        FotoRestaurante foto = propia(restaurant, fotoId);
        if (foto.getTipo() == TipoFoto.FRENTE) {
            throw new BusinessException("La foto del frente no se puede borrar, solo reemplazar.");
        }
        long salones = fotoRepo.findAllByRestaurant_Id(restaurant.getId()).stream()
                .filter(f -> f.getTipo() == TipoFoto.SALON).count();
        if (salones <= MIN_SALON) {
            throw new BusinessException(
                    "Tenés que tener al menos %d fotos del salón. Subí otra antes de borrar esta.".formatted(MIN_SALON));
        }
        restaurant.getFotos().remove(foto);
        fotoRepo.delete(foto);
        borrarDespuesDelCommit(foto.getStorageKey());
    }

    /** Borra solo los archivos; las filas se van por cascada cuando se elimina el restaurante. */
    public void eliminarArchivos(Restaurant restaurant) {
        fotoRepo.findAllByRestaurant_Id(restaurant.getId())
                .forEach(f -> borrarDespuesDelCommit(f.getStorageKey()));
    }

    private FotoRestaurante propia(Restaurant restaurant, Long fotoId) {
        FotoRestaurante foto = fotoRepo.findById(fotoId)
                .orElseThrow(() -> new EntityNotFoundException("Foto no encontrada"));
        if (!foto.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ForbiddenException("La foto no pertenece a tu restaurante");
        }
        return foto;
    }

    private FotoRestaurante guardarNueva(Restaurant restaurant, TipoFoto tipo, Imagen imagen) {
        FotoRestaurante foto = new FotoRestaurante();
        foto.setRestaurant(restaurant);
        foto.setTipo(tipo);
        foto.setStorageKey(subir(restaurant, imagen));
        foto.setCreadaEn(Instant.now());
        restaurant.getFotos().add(foto);
        return fotoRepo.saveAndFlush(foto);
    }

    /** Sube el archivo y, si la transacción no se confirma, lo borra para no dejar huérfanos. */
    private String subir(Restaurant restaurant, Imagen imagen) {
        String key = "fotos/%d/%s.%s".formatted(restaurant.getId(), UUID.randomUUID(), imagen.extension());
        storage.upload(key, imagen.contenido(), imagen.contentType());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        borrarSinFallar(key);
                    }
                }
            });
        }
        return key;
    }

    private FotoDTO toDto(FotoRestaurante foto) {
        return new FotoDTO(foto.getId(), foto.getTipo(), storage.publicUrl(foto.getStorageKey()));
    }

    private record Imagen(byte[] contenido, String contentType, String extension) {}

    /** Valida por contenido (los primeros bytes), no por extensión ni por el tipo que manda el navegador. */
    private static Imagen validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Falta una foto");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new BusinessException("Cada foto puede pesar hasta 5 MB");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("No pudimos leer la foto");
        }
        if (empiezaCon(contenido, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return new Imagen(contenido, "image/jpeg", "jpg");
        }
        if (empiezaCon(contenido, 0, new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return new Imagen(contenido, "image/png", "png");
        }
        if (empiezaCon(contenido, 0, ascii("RIFF")) && empiezaCon(contenido, 8, ascii("WEBP"))) {
            return new Imagen(contenido, "image/webp", "webp");
        }
        throw new BusinessException("Las fotos tienen que ser JPG, PNG o WebP");
    }

    private static boolean empiezaCon(byte[] contenido, int desde, byte[] firma) {
        if (contenido.length < desde + firma.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOfRange(contenido, desde, desde + firma.length), firma);
    }

    private static byte[] ascii(String texto) {
        return texto.getBytes(StandardCharsets.US_ASCII);
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

    /** Un archivo huérfano no hace daño; fallar el pedido después de cambiar la base, sí. */
    private void borrarSinFallar(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar el archivo {} del almacenamiento", key, e);
        }
    }
}
