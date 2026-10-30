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
 * Fotos de un restaurante: entre 3 y 5, y exactamente una es la portada (la miniatura del catálogo), elegida por
 * el encargado. Los restaurantes registrados antes de esta regla pueden tener menos y completarlas desde el panel.
 */
@Service
public class FotoService {

    public static final int MIN_FOTOS = 3;
    public static final int MAX_FOTOS = 5;
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(FotoService.class);
    private static final Comparator<FotoRestaurante> ORDEN =
            Comparator.comparing(FotoRestaurante::isPortada).reversed().thenComparing(FotoRestaurante::getId);

    private final FotoRestauranteRepository fotoRepo;
    private final FileStorage storage;

    public FotoService(FotoRestauranteRepository fotoRepo, FileStorage storage) {
        this.fotoRepo = fotoRepo;
        this.storage = storage;
    }

    /** Valida las fotos del registro antes de crear nada, para no dejar un restaurante a medias. */
    public static void validarRegistro(List<MultipartFile> fotos, Integer portada) {
        List<MultipartFile> subidas = noVacias(fotos);
        if (subidas.size() < MIN_FOTOS || subidas.size() > MAX_FOTOS) {
            throw new BusinessException("Subí entre %d y %d fotos del restaurante".formatted(MIN_FOTOS, MAX_FOTOS));
        }
        if (portada == null || portada < 0 || portada >= subidas.size()) {
            throw new BusinessException("Elegí cuál de las fotos es la portada");
        }
        subidas.forEach(FotoService::validarImagen);
    }

    /** Guarda las fotos del registro. Se asume que {@link #validarRegistro} ya pasó. */
    @Transactional
    public void guardarRegistro(Restaurant restaurant, List<MultipartFile> fotos, int portada) {
        List<MultipartFile> subidas = noVacias(fotos);
        for (int i = 0; i < subidas.size(); i++) {
            guardarNueva(restaurant, i == portada, validarImagen(subidas.get(i)));
        }
    }

    @Transactional(readOnly = true)
    public List<FotoDTO> listar(Long restaurantId) {
        return fotoRepo.findAllByRestaurant_Id(restaurantId).stream().sorted(ORDEN).map(this::toDto).toList();
    }

    /** URL de la portada de cada restaurante; los que no tienen fotos quedan afuera del mapa. */
    @Transactional(readOnly = true)
    public Map<Long, String> portadas(Collection<Long> restaurantIds) {
        if (restaurantIds.isEmpty()) {
            return Map.of();
        }
        return fotoRepo.findAllByRestaurant_IdInAndPortadaTrue(restaurantIds).stream()
                .collect(Collectors.toMap(f -> f.getRestaurant().getId(), f -> storage.publicUrl(f.getStorageKey()),
                        (a, b) -> a));
    }

    /** Si el restaurante todavía no tiene portada, la foto que sube queda como portada. */
    @Transactional
    public FotoDTO agregar(Restaurant restaurant, MultipartFile archivo) {
        List<FotoRestaurante> actuales = fotoRepo.findAllByRestaurant_Id(restaurant.getId());
        if (actuales.size() >= MAX_FOTOS) {
            throw new BusinessException("Podés tener hasta %d fotos. Borrá una antes de subir otra.".formatted(MAX_FOTOS));
        }
        boolean sinPortada = actuales.stream().noneMatch(FotoRestaurante::isPortada);
        return toDto(guardarNueva(restaurant, sinPortada, validarImagen(archivo)));
    }

    /** Marca la foto como portada y desmarca la anterior. Devuelve las fotos en el orden en que se muestran. */
    @Transactional
    public List<FotoDTO> elegirPortada(Restaurant restaurant, Long fotoId) {
        FotoRestaurante elegida = propia(restaurant, fotoId);
        List<FotoRestaurante> fotos = fotoRepo.findAllByRestaurant_Id(restaurant.getId());
        fotos.forEach(f -> f.setPortada(f.getId().equals(elegida.getId())));
        fotoRepo.saveAllAndFlush(fotos);
        return fotos.stream().sorted(ORDEN).map(this::toDto).toList();
    }

    /** Cambia la imagen manteniendo si es portada. El archivo anterior se borra una vez confirmado el cambio. */
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
        if (foto.isPortada()) {
            throw new BusinessException("La portada no se puede borrar. Elegí otra portada antes de borrar esta.");
        }
        if (fotoRepo.findAllByRestaurant_Id(restaurant.getId()).size() <= MIN_FOTOS) {
            throw new BusinessException(
                    "Tenés que tener al menos %d fotos. Subí otra antes de borrar esta.".formatted(MIN_FOTOS));
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

    private FotoRestaurante guardarNueva(Restaurant restaurant, boolean portada, Imagen imagen) {
        FotoRestaurante foto = new FotoRestaurante();
        foto.setRestaurant(restaurant);
        foto.setPortada(portada);
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
        return new FotoDTO(foto.getId(), storage.publicUrl(foto.getStorageKey()), foto.isPortada());
    }

    private static List<MultipartFile> noVacias(List<MultipartFile> fotos) {
        return fotos == null ? List.of() : fotos.stream().filter(f -> !f.isEmpty()).toList();
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
