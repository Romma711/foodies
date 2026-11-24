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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Applies the deactivation of restaurants (Phase 3, D7) without touching the reservation service.
 *
 * <ul>
 *   <li>{@code POST /api/reservas} and {@code POST /api/lista-espera}: reads {@code idRestaurant}
 *       from the body; if the restaurant is approved but not published (deactivated, or its manager
 *       account was deactivated) it answers 404 with the same body the API gives when the restaurant
 *       does not exist, because such a restaurant is hidden from every public query.</li>
 *   <li>Writes to the manager panel ({@code /api/restaurantes/me/**} with any method but GET, and
 *       {@code PATCH /api/reservas/{id}/estado}): if the manager's restaurant was deactivated it
 *       answers 403. The panel stays readable.</li>
 * </ul>
 * In any other case the request goes on untouched.
 */
@Component
public class ReservaRestauranteActivoFilter extends OncePerRequestFilter {

    private static final String RESERVAS = "/api/reservas";
    private static final String LISTA_ESPERA = "/api/lista-espera";
    private static final String PANEL = "/api/restaurantes/me";
    private static final Pattern ESTADO_RESERVA = Pattern.compile("^/api/reservas/[^/]+/estado$");

    private final RestaurantRepository restaurantRepo;
    private final ObjectMapper objectMapper;

    public ReservaRestauranteActivoFilter(RestaurantRepository restaurantRepo, ObjectMapper objectMapper) {
        this.restaurantRepo = restaurantRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !esAlta(request) && !esEscrituraDelPanel(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (esEscrituraDelPanel(request)) {
            if (restauranteDelEncargadoDadoDeBaja()) {
                escribir(response, HttpServletResponse.SC_FORBIDDEN, Map.of(
                        "error", "prohibido",
                        "mensaje", "Tu restaurante fue dado de baja: el panel queda en modo lectura. "
                                + "Escribinos si creés que es un error."));
                return;
            }
            chain.doFilter(request, response);
            return;
        }
        // The controller still needs to read the body, so it is buffered once.
        CuerpoReleible releible = new CuerpoReleible(request);
        if (restauranteNoPublicado(releible.cuerpo())) {
            escribir(response, HttpServletResponse.SC_NOT_FOUND, Map.of(
                    "error", "entidad no encontrada",
                    "mensaje", "Restaurante no encontrado"));
            return;
        }
        chain.doFilter(releible, response);
    }

    private static boolean esAlta(HttpServletRequest request) {
        String path = request.getServletPath();
        return HttpMethod.POST.matches(request.getMethod()) && (RESERVAS.equals(path) || LISTA_ESPERA.equals(path));
    }

    private static boolean esEscrituraDelPanel(HttpServletRequest request) {
        String path = request.getServletPath();
        String metodo = request.getMethod();
        if (HttpMethod.PATCH.matches(metodo) && ESTADO_RESERVA.matcher(path).matches()) {
            return true;
        }
        boolean lectura = HttpMethod.GET.matches(metodo) || HttpMethod.HEAD.matches(metodo)
                || HttpMethod.OPTIONS.matches(metodo);
        return !lectura && (PANEL.equals(path) || path.startsWith(PANEL + "/"));
    }

    private boolean restauranteDelEncargadoDadoDeBaja() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return restaurantRepo.findByUsuario_Email(auth.getName())
                .map(restaurant -> Boolean.FALSE.equals(restaurant.getActivo()))
                .orElse(false);
    }

    private boolean restauranteNoPublicado(byte[] cuerpo) {
        try {
            // asLong also reads "5": Jackson binds a quoted id to the DTO, so it must be checked too.
            JsonNode id = objectMapper.readTree(cuerpo).path("idRestaurant");
            return restaurantRepo.findById(id.asLong(-1))
                    .map(restaurant -> restaurant.isAprobado() && !restaurant.isPublicado())
                    .orElse(false);
        } catch (IOException ex) {
            // Malformed body: let the endpoint answer as it always did.
            return false;
        }
    }

    private void escribir(HttpServletResponse response, int status, Map<String, String> cuerpo) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
