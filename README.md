Foodies - Sistema de Reserva de Restaurantes

Foodies es una aplicación web desarrollada en Java con Spring Boot y MySQL, que permite gestionar reservas de restaurantes, subir cartas gastronómicas en formato PDF, y administrar usuarios con autenticación JWT y control de acceso por roles.

🌐 Tecnologías utilizadas

Java 21

Spring Boot 3.4.5

Spring Web

Spring Data JPA

Spring Security

Spring Validation

MySQL

JWT (JSON Web Tokens)

MapStruct

Maven

Postman (para pruebas de API)

🧱 Arquitectura del Proyecto

El sistema sigue el patrón de arquitectura MVC (Model-View-Controller) y está dividido en:

Entidad y Repositorio: Modelo de datos + JpaRepository

Servicios: Contienen la lógica de negocio

DTOs: Transporte de datos seguro y eficiente

Controladores: Exponen la API REST

Seguridad: Basada en JWT, roles y filtros personalizados

Los usuarios pueden ser CLIENTE, ENCARGADO o ADMIN, y cada uno tiene permisos distintos para acceder a los endpoints.

🔐 Autenticación y Roles

La seguridad está implementada con:

Spring Security

JWT con filtro personalizado (JwtAuthFilter)

Codificación de contraseñas con BCrypt

Usuarios: Cliente, Encargado, Admin

@PreAuthorize("hasRole('ADMIN')")

🚀 Instalación y Ejecución

Prerrequisitos

Java 21

Maven

MySQL

Pasos

Clonar el repositorio:

git clone https://github.com/usuario/foodies.git

Crear la base de datos en MySQL:

CREATE DATABASE foodies;

Configurar application.properties:

spring.datasource.url=jdbc:mysql://localhost:3306/foodies
spring.datasource.username=root
spring.datasource.password=asd123

Ejecutar el proyecto desde IntelliJ o con:

./mvnw spring-boot:run

🌐 API REST - Documentación de Endpoints

🏥 RESTAURANTES

Verbo

Endpoint

Descripción

GET

/api/restaurante/all

Lista todos los restaurantes

GET

/api/restaurante/especialidad?especialidadDeComida=PASTAS

Filtra por especialidad

GET

/api/restaurante/{id}

Buscar restaurante por ID

POST

/api/auth/register/restaurant

Registrar restaurante

PATCH

/api/restaurante/actualizar/{id}

Actualizar datos

DELETE

/api/restaurante/eliminar/{id}

Eliminar restaurante

🍽️ CARTAS

Verbo

Endpoint

Descripción

POST

/api/carta/subir

Subir carta (PDF)

GET

/api/carta/{id}/descargar

Ver o descargar carta

PUT

/api/carta/actualizar

Actualizar carta PDF

DELETE

/api/carta/{id}

Eliminar carta asociada

📕 RESERVAS

Verbo

Endpoint

Descripción

POST

/api/reserva/create

Crear nueva reserva

GET

/api/reserva/list

Listar todas las reservas

DELETE

/api/reserva/{id}

Eliminar reserva por ID

📄 RESEÑAS

Verbo

Endpoint

Descripción

POST

/api/resena/create

Crear nueva reseña

GET

/api/resena/list?id={restaurantId}

Listar reseñas por restaurante

PUT

/api/resena/{id}/actualizar

Actualizar reseña

DELETE

/api/resena/{id}/eliminar

Eliminar reseña

🔑 AUTENTICACIÓN

Verbo

Endpoint

Descripción

POST

/api/auth/login

Login y obtención de token JWT

POST

/api/auth/register/cliente

Registro de nuevo cliente

POST

/api/auth/register/restaurant

Registro de restaurante (encargado)

👮 ADMINISTRADOR

Verbo

Endpoint

Descripción

PUT

/api/admin/{usuarioId}/aprobar

Aprobar cuenta de encargado

🎡 Roles y Permisos

CLIENTE

Crear reservas

Crear reseñas

ENCARGADO

Subir carta PDF

Modificar restaurante propio

ADMIN

Aprobar encargados

Acceso completo

📄 Datos de Prueba (Postman)

Registro de Cliente

POST /api/auth/register/cliente
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@gmail.com",
  "password": "1234",
  "telefono": "11334455"
}

Login

POST /api/auth/login
{
  "email": "juan@gmail.com",
  "password": "1234"
}

Respuesta: Bearer <token>

📢 Autor

Desarrollado por Juan Estavillo como parte del trabajo final de la materia.

🚧 Licencia

Este proyecto es de uso educativo y libre distribución para fines académicos.

Para cualquier consulta podés contactarme vía GitHub o correo electrónico. 🚀
