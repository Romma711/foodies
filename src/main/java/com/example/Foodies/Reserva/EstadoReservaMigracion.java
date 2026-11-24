package com.example.Foodies.Reserva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.util.List;

/**
 * On MySQL, turns {@code reserva.estado_reserva} from a native {@code enum(...)} into
 * {@code varchar(20)}.
 *
 * <p>Hibernate created the column as an enum with the three original states, and
 * {@code ddl-auto=update} never alters an existing column, so saving RECHAZADA, ASISTIO,
 * NO_ASISTIO or VENCIDA would fail. Existing values are kept as the same text. It is idempotent and
 * does nothing on other databases (tests use H2 with {@code create-drop}).</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EstadoReservaMigracion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EstadoReservaMigracion.class);

    private final JdbcTemplate jdbc;

    public EstadoReservaMigracion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String producto = jdbc.execute((java.sql.Connection c) -> {
            DatabaseMetaData meta = c.getMetaData();
            return meta.getDatabaseProductName();
        });
        if (producto == null || !producto.toLowerCase().contains("mysql")) {
            return;
        }
        List<String> tipos = jdbc.queryForList("""
                select data_type from information_schema.columns
                where table_schema = database() and lower(table_name) = 'reserva' and column_name = 'estado_reserva'
                """, String.class);
        if (tipos.isEmpty() || !"enum".equalsIgnoreCase(tipos.get(0))) {
            return;
        }
        jdbc.execute("alter table reserva modify estado_reserva varchar(20) not null");
        log.info("Reservas: estado_reserva pasó de enum a varchar(20) para admitir los estados nuevos");
    }
}
