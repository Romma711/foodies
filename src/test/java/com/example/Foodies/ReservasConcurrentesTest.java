package com.example.Foodies;

import com.example.Foodies.AutorizacionTestSupport.Cliente;
import com.example.Foodies.AutorizacionTestSupport.Encargado;
import com.example.Foodies.Reserva.Reserva;
import com.example.Foodies.Reserva.ReservaRepository;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D17 / D1 under concurrency with the isolation MySQL uses by default (REPEATABLE READ). H2 runs
 * READ COMMITTED by default, which hides stale snapshots, so this class forces REPEATABLE READ on
 * its own in-memory database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservasConcurrentesTest {

    @TempDir
    static Path storageDir;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("storage.provider", () -> "local");
        registry.add("storage.local.dir", () -> storageDir.toString());
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:foodies-rr;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=10000");
        registry.add("spring.datasource.hikari.transaction-isolation", () -> "TRANSACTION_REPEATABLE_READ");
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private RestaurantRepository restaurantRepo;
    @Autowired
    private ReservaRepository reservaRepo;

    @Test
    void diezReservasSimultaneasRespetanElCupoYLosDuplicados() throws Exception {
        AutorizacionTestSupport api = new AutorizacionTestSupport(rest, restaurantRepo);
        Encargado encargado = api.nuevoEncargado();
        Long espacio = api.primerEspacio(encargado.restaurantId());
        LocalDate fecha = LocalDate.now().plusDays(3);
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            clientes.add(api.nuevoCliente());
        }
        // Two clients send their booking twice.
        List<Cliente> pedidos = new ArrayList<>(clientes);
        pedidos.add(clientes.get(0));
        pedidos.add(clientes.get(1));

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(pedidos.size());
        int ok = 0;
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (Cliente cliente : pedidos) {
                Callable<Integer> reservar = () -> {
                    largada.await();
                    return api.reservar(cliente, encargado.restaurantId(), espacio, fecha, 3).getStatusCode().value();
                };
                resultados.add(pool.submit(reservar));
            }
            largada.countDown();
            for (Future<Integer> resultado : resultados) {
                if (resultado.get() == 200) {
                    ok++;
                }
            }
        } finally {
            pool.shutdownNow();
        }

        List<Reserva> activas = reservaRepo.findAllByRestaurant_Id(encargado.restaurantId()).stream()
                .filter(r -> r.getEstadoReserva().esActiva())
                .toList();
        // Capacity 10: three bookings of 3 fit; nobody holds two.
        assertThat(ok).isEqualTo(3);
        assertThat(activas.stream().mapToInt(Reserva::getCantidad).sum()).isEqualTo(9);
        Map<Long, Long> porCliente = activas.stream()
                .collect(Collectors.groupingBy(r -> r.getUsuario().getId(), Collectors.counting()));
        assertThat(porCliente.values()).allMatch(n -> n == 1);
    }
}
