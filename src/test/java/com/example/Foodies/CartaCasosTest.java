package com.example.Foodies;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Casos de la carta: que solo entren PDFs de verdad (Content-Type + firma %PDF-),
 * permisos por rol y dueño, y el ciclo subir / descargar / actualizar / eliminar.
 *
 * Usa MockMvc porque solo así puedo mandarle al servidor un archivo con un
 * Content-Type exacto (TestRestTemplate no armaba bien la parte "archivo").
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CartaCasosTest extends ApiTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private static final byte[] PDF_DE_VERDAD = "%PDF-1.4\n% contenido de prueba\n%%EOF".getBytes();
    private static final byte[] PDF_ACTUALIZADO = "%PDF-1.7\n% carta nueva\n%%EOF".getBytes();
    private static final byte[] BASURA = "esto no es un pdf".getBytes();

    private static MockMultipartFile archivo(String contentType, byte[] contenido) {
        return new MockMultipartFile("archivo", "carta.pdf", contentType, contenido);
    }

    private ResultActions subir(String token, MockMultipartFile file, Long restoId) throws Exception {
        return mockMvc.perform(multipart("/api/carta")
                .file(file)
                .param("restaurantId", String.valueOf(restoId))
                .header("Authorization", "Bearer " + token));
    }

    @Test
    void cicloCompletoDeLaCarta() throws Exception {
        Long resto = registrarResto("carta-ciclo@test.com", "RestoCicloCarta", 10, "PARRILLA", true);
        String token = login("carta-ciclo@test.com", "secret1");

        // subir un PDF de verdad
        subir(token, archivo("application/pdf", PDF_DE_VERDAD), resto)
                .andExpect(status().isOk());

        // descargar: vuelve con el mismo contenido
        MvcResult descarga = mockMvc.perform(MockMvcRequestBuilders.get("/api/carta/{id}", resto))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andReturn();
        assertThat(descarga.getResponse().getContentAsByteArray()).isEqualTo(PDF_DE_VERDAD);

        // no se puede subir una segunda carta
        subir(token, archivo("application/pdf", PDF_DE_VERDAD), resto)
                .andExpect(status().isBadRequest());

        // actualizar si se puede
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/carta")
                        .file(archivo("application/pdf", PDF_ACTUALIZADO))
                        .param("restaurantId", String.valueOf(resto))
                        .header("Authorization", "Bearer " + token)
                        // el update de la carta es PUT
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());

        MvcResult descargaNueva = mockMvc.perform(MockMvcRequestBuilders.get("/api/carta/{id}", resto))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(descargaNueva.getResponse().getContentAsByteArray()).isEqualTo(PDF_ACTUALIZADO);

    }

    @Test
    void eliminarLaCartaLaBorraDeLaBase() throws Exception {
        Long resto = registrarResto("carta-borrado@test.com", "RestoCartaBorrado", 10, "MINUTAS", true);
        String token = login("carta-borrado@test.com", "secret1");

        subir(token, archivo("application/pdf", PDF_DE_VERDAD), resto)
                .andExpect(status().isOk());

        ResponseEntity<String> borrada = rest.exchange("/api/carta/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(token, false)), String.class, resto);
        assertThat(borrada.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/carta/{id}", resto))
                .andExpect(status().isNotFound());

        // y se puede volver a subir
        subir(token, archivo("application/pdf", PDF_DE_VERDAD), resto)
                .andExpect(status().isOk());
    }

    @Test
    void soloEntranArchivosQueSonPdfDeVerdad() throws Exception {
        Long resto = registrarResto("carta-pdf@test.com", "RestoSoloPdf", 10, "CAFE", true);
        String token = login("carta-pdf@test.com", "secret1");

        // dice que es pdf pero el contenido es basura -> 400 con mensaje claro
        subir(token, archivo("application/pdf", BASURA), resto)
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("Debe subir un archivo PDF válido"));

        // es pdf de verdad pero viene como texto -> 400
        subir(token, archivo("text/plain", PDF_DE_VERDAD), resto)
                .andExpect(status().isBadRequest());

        // sin content-type en la parte -> 400 (antes tiraba 500 por NPE)
        subir(token, new MockMultipartFile("archivo", "carta.pdf", null, PDF_DE_VERDAD), resto)
                .andExpect(status().isBadRequest());

        // archivo vacio -> 400
        subir(token, archivo("application/pdf", new byte[0]), resto)
                .andExpect(status().isBadRequest());

        // y todavia no se guardo ninguna carta
        mockMvc.perform(MockMvcRequestBuilders.get("/api/carta/{id}", resto))
                .andExpect(status().isNotFound());

        // sin archivo la parte directamente no llega -> 400
        mockMvc.perform(multipart("/api/carta")
                        .param("restaurantId", String.valueOf(resto))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laCartaRespetaRolesYDuena() throws Exception {
        Long restoA = registrarResto("carta-duenio-a@test.com", "RestoCartaA", 10, "MINUTAS", true);
        Long restoB = registrarResto("carta-duenio-b@test.com", "RestoCartaB", 10, "MINUTAS", true);
        String tokenA = login("carta-duenio-a@test.com", "secret1");
        String tokenB = login("carta-duenio-b@test.com", "secret1");
        registrarCliente("carta-cliente@test.com", "Nico");
        String tokenCliente = login("carta-cliente@test.com", "secret1");

        // sin token -> 401
        mockMvc.perform(multipart("/api/carta")
                        .file(archivo("application/pdf", PDF_DE_VERDAD))
                        .param("restaurantId", String.valueOf(restoA)))
                .andExpect(status().isUnauthorized());

        // un cliente no puede subir carta -> 403
        subir(tokenCliente, archivo("application/pdf", PDF_DE_VERDAD), restoA)
                .andExpect(status().isForbidden());

        // un encargado no puede subir carta a un restaurante ajeno -> 403
        subir(tokenB, archivo("application/pdf", PDF_DE_VERDAD), restoA)
                .andExpect(status().isForbidden());

        // el dueño si
        subir(tokenA, archivo("application/pdf", PDF_DE_VERDAD), restoA)
                .andExpect(status().isOk());

        // cualquiera puede descargarla (es publica)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/carta/{id}", restoA))
                .andExpect(status().isOk());

        // pero borrarla solo puede el dueño o el admin (y va por restaurantId)
        ResponseEntity<String> borrarAjena = rest.exchange("/api/carta/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenB, false)), String.class, restoA);
        assertThat(borrarAjena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> borrarAdmin = rest.exchange("/api/carta/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(adminToken(), false)), String.class, restoA);
        assertThat(borrarAdmin.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // restaurante inexistente -> 404
        ResponseEntity<String> borrarInexistente = rest.exchange("/api/carta/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenA, false)), String.class, 999999L);
        assertThat(borrarInexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // restaurante sin carta -> 404 tambien
        ResponseEntity<String> borrarSinCarta = rest.exchange("/api/carta/{id}", HttpMethod.DELETE,
                new HttpEntity<>(auth(tokenB, false)), String.class, restoB);
        assertThat(borrarSinCarta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // restaurante sin carta -> 404
        mockMvc.perform(MockMvcRequestBuilders.get("/api/carta/{id}", restoB))
                .andExpect(status().isNotFound());
    }
}
