package com.example.Foodies;

/**
 * Base de los tests: define la configuración obligatoria que la aplicación exige
 * al arrancar (JWT_SECRET y ADMIN_PASSWORD) antes de que Spring cree el contexto.
 * JwtSecretConfig y PasswordConfig fallan si alguna no está definida.
 *
 * Se setea como propiedad de sistema para que funcione igual con Maven y con
 * los tests lanzados desde el IDE. El valor de ADMIN_PASSWORD es SOLO de tests.
 */
abstract class TestBase {

    static {
        System.setProperty("JWT_SECRET", "clave-de-test-foodies-0123456789-0123456789");
        System.setProperty("ADMIN_PASSWORD", "admin123");
    }
}
