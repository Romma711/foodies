package com.example.Foodies.CupoEspecial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.util.List;

/**
 * Al arrancar en MySQL, quita la clave única vieja de {@code CuposEspeciales} sobre
 * {@code (restaurant_id, fecha)}.
 *
 * <p>Ahora el cupo especial es por espacio y la clave única es {@code (espacio_id, fecha)}.
 * {@code ddl-auto=update} agrega la nueva pero no borra la vieja, que impediría cargar un cupo
 * especial en dos espacios del mismo restaurante el mismo día. Antes de borrarla crea un índice
 * simple sobre {@code restaurant_id}, porque MySQL la puede estar usando para la clave foránea.
 * Es idempotente y no hace nada en otras bases (los tests usan H2 con {@code create-drop}).</p>
 */
@Component
public class CuposEspecialesUnicidadMigracion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CuposEspecialesUnicidadMigracion.class);
    private static final String INDICE_RESTAURANT = "idx_cupos_especiales_restaurant";

    private final JdbcTemplate jdbc;

    public CuposEspecialesUnicidadMigracion(JdbcTemplate jdbc) {
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
        List<String> tablas = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = database() and lower(table_name) = 'cuposespeciales'
                """, String.class);
        if (tablas.isEmpty()) {
            return;
        }
        String tabla = tablas.get(0);
        List<String> viejas = jdbc.queryForList("""
                select index_name from information_schema.statistics
                where table_schema = database() and table_name = ? and non_unique = 0 and index_name <> 'PRIMARY'
                group by index_name
                having sum(column_name = 'restaurant_id') = 1 and sum(column_name = 'espacio_id') = 0
                """, String.class, tabla);
        if (viejas.isEmpty()) {
            return;
        }
        Integer indiceSimple = jdbc.queryForObject("""
                select count(*) from information_schema.statistics
                where table_schema = database() and table_name = ? and index_name = ?
                """, Integer.class, tabla, INDICE_RESTAURANT);
        if (indiceSimple == null || indiceSimple == 0) {
            jdbc.execute("create index " + INDICE_RESTAURANT + " on `" + tabla + "` (restaurant_id)");
        }
        for (String indice : viejas) {
            jdbc.execute("alter table `" + tabla + "` drop index `" + indice + "`");
            log.info("Cupos especiales: se quitó la clave única vieja {} (restaurant_id, fecha)", indice);
        }
    }
}
