package com.example.Foodies.Moderacion;

import com.example.Foodies.Restaurant.RestaurantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Impide reservar en un restaurante dado de baja (Fase 3) sin modificar el servicio de reservas.
 *
 * <p>Para {@code POST /api/reservas} lee {@code idRestaurant} del cuerpo; si el restaurante fue dado
 * de baja responde 404 con el mismo cuerpo que da la API cuando el restaurante no existe, porque un
 * restaurante dado de baja se oculta en todas las consultas públicas. En cualquier otro caso la
 * request sigue intacta y el servicio de reservas aplica sus validaciones de siempre.</p>
 */
@Component
public class ReservaRestauranteActivoFilter extends OncePerRequestFilter {

    private static final String RESERVAS = "/api/reservas";

    private final RestaurantRepository restaurantRepo;
    private final ObjectMapper objectMapper;

    public ReservaRestauranteActivoFilter(RestaurantRepository restaurantRepo, ObjectMapper objectMapper) {
        this.restaurantRepo = restaurantRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && RESERVAS.equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // The reservation controller still needs to read the body, so it is buffered once.
        CuerpoReleible releible = new CuerpoReleible(request);
        if (restauranteDadoDeBaja(releible.cuerpo())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "error", "entidad no encontrada",
                    "mensaje", "Restaurante no encontrado"));
            return;
        }
        chain.doFilter(releible, response);
    }

    private boolean restauranteDadoDeBaja(byte[] cuerpo) {
        try {
            JsonNode id = objectMapper.readTree(cuerpo).path("idRestaurant");
            if (!id.canConvertToLong()) {
                return false;
            }
            return restaurantRepo.findById(id.asLong())
                    .map(restaurant -> Boolean.FALSE.equals(restaurant.getActivo()))
                    .orElse(false);
        } catch (IOException ex) {
            // Malformed body: let the reservation endpoint answer as it always did.
            return false;
        }
    }
}
