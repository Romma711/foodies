package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** S3: accounts are managed by the admin; a client only reaches its own account. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClienteAutorizacionTest {

    @TempDir
    static Path storageDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("storage.provider", () -> "local");
        registry.add("storage.local.dir", () -> storageDir.toString());
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private RestaurantRepository restaurantRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    private AutorizacionTestSupport api;

    @BeforeEach
    void setUp() {
        api = new AutorizacionTestSupport(rest, restaurantRepo);
    }

    @Test
    void soloElAdminListaLasCuentas() {
        Cliente cliente = api.nuevoCliente();

        assertThat(api.status(HttpMethod.GET, "/api/clientes", cliente.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.GET, "/api/clientes", api.admin(), null)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unClienteNoVeNiModificaNiBorraOtraCuenta() {
        Cliente cliente = api.nuevoCliente();
        Cliente otro = api.nuevoCliente();
        String path = "/api/clientes/" + otro.id();

        assertThat(api.status(HttpMethod.GET, path, cliente.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.PATCH, path, cliente.token(),
                Map.of("nombre", "Hackeado", "apellido", "X", "telefono", "1"))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.status(HttpMethod.DELETE, path, cliente.token(), null)).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(usuarioRepo.findById(otro.id())).hasValueSatisfying(u -> assertThat(u.getNombre()).isEqualTo("Cliente"));
    }

    @Test
    void unClienteVeYModificaSuPropiaCuenta() {
        Cliente cliente = api.nuevoCliente();
        String path = "/api/clientes/" + cliente.id();

        assertThat(api.status(HttpMethod.GET, path, cliente.token(), null)).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> patch = api.send(HttpMethod.PATCH, path, cliente.token(),
                Map.of("nombre", "Ana", "apellido", "Gómez", "telefono", "2235555555"), Map.class);
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patch.getBody()).containsEntry("nombre", "Ana").containsEntry("rol", "ROLE_CLIENTE");
    }

    @Test
    void elPatchSoloCambiaLosCamposQueRecibe() {
        Cliente cliente = api.nuevoCliente();
        String apellidoOriginal = usuarioRepo.findById(cliente.id()).orElseThrow().getApellido();

        ResponseEntity<Map> patch = api.send(HttpMethod.PATCH, "/api/clientes/" + cliente.id(), cliente.token(),
                Map.of("telefono", "2234444444"), Map.class);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patch.getBody()).containsEntry("nombre", "Cliente").containsEntry("telefono", "2234444444");
        assertThat(usuarioRepo.findById(cliente.id())).hasValueSatisfying(u -> {
            assertThat(u.getNombre()).isEqualTo("Cliente");
            assertThat(u.getApellido()).isEqualTo(apellidoOriginal);
            assertThat(u.getTelefono()).isEqualTo("2234444444");
        });
    }

    @Test
    void unClienteNoPuedeCambiarSuRolNiSuEstadoConPatch() {
        Cliente cliente = api.nuevoCliente();

        ResponseEntity<Map> patch = api.send(HttpMethod.PATCH, "/api/clientes/" + cliente.id(), cliente.token(),
                Map.of("nombre", "Ana", "apellido", "Gómez", "telefono", "2235555555",
                        "rol", "ROLE_ADMIN", "activo", false, "strikes", 0), Map.class);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(usuarioRepo.findById(cliente.id())).hasValueSatisfying(u -> {
            assertThat(u.getRol()).isEqualTo(Role.ROLE_CLIENTE);
            assertThat(u.getActivo()).isTrue();
        });
    }

    @Test
    void elAdminVeYModificaCualquierCuenta() {
        Cliente cliente = api.nuevoCliente();
        String admin = api.admin();
        String path = "/api/clientes/" + cliente.id();

        assertThat(api.status(HttpMethod.GET, path, admin, null)).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.PATCH, path, admin,
                Map.of("nombre", "Editado", "apellido", "Admin", "telefono", "2230000001"))).isEqualTo(HttpStatus.OK);
        assertThat(api.status(HttpMethod.DELETE, path, admin, null)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usuarioRepo.findById(cliente.id())).isEmpty();
    }
}
