<h1><em> Foodies - Sistema de Reserva de Restaurantes </em></h1>
<h3> Integrantes: </h3>
<h5> Ramiro Sacchetta </h5>
<h5> Andres Roma </h5>
<h5> Juan Estavillo </h5>
<p>Foodies es una aplicación web desarrollada en Java con Spring Boot y MySQL, que permite gestionar reservas de restaurantes, subir cartas gastronómicas en formato PDF, y administrar usuarios con autenticación JWT y control de acceso por roles.</p>

<h2>🌐 Tecnologías utilizadas</h2>

- Java 21
- Spring Boot 3.4.5
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- MySQL
- JWT (JSON Web Tokens)
- MapStruct
- Maven
- Postman (para pruebas de API)

<h2>🧱 Arquitectura del Proyecto</h2>

<h4>El sistema sigue el patrón de arquitectura MVC (Model-View-Controller) y está dividido en:</h4>

- Entidad y Repositorio: Modelo de datos + JpaRepository

- Servicios: Contienen la lógica de negocio

- DTOs: Transporte de datos seguro y eficiente

- Controladores: Exponen la API REST

- Seguridad: Basada en JWT, roles y filtros personalizados

- Los usuarios pueden ser CLIENTE, ENCARGADO o ADMIN, y cada uno tiene permisos distintos para acceder a los endpoints.

<h2>🔐 Autenticación y Roles</h2>

<h4>La seguridad está implementada con:</h4>

- Spring Security

- JWT con filtro personalizado (JwtAuthFilter)

- Codificación de contraseñas con BCrypt

- Usuarios: Cliente, Encargado, Admin
@PreAuthorize("hasRole('ADMIN')")

<h2>🚀 Instalación y Ejecución</h2>

<h4>Prerrequisitos</h4>

- Java 21

- Maven

- MySQL

<h4>Pasos</h4>

- Clonar el repositorio:
git clone https://github.com/usuario/foodies.git

- Crear la base de datos en MySQL:

- CREATE DATABASE foodies;

- Configurar application.properties:
```
spring.datasource.url=jdbc:mysql://localhost:3306/foodies
spring.datasource.username=<tu-usuario>
spring.datasource.password=<tu-contraseña>
```
- Ejecutar el proyecto desde IntelliJ o con:
./mvnw spring-boot:run

<h2>🌐 API REST - Documentación de Endpoints</h2>

## 👦 CLIENTE
>`GET`
>
>`/api/cliente/all`
>- Lista todos los clientes
###
>`GET`
>
>`/api/cliente/{id}`
>- Lista al cliente solicitado
###
>`DELETE`
>
>`/api/cliente/{id}`
>- Elimina al cliente solicitado
###
>`PATCH`

```
/api/cliente/{id}

> Body
{
  "nombre": "",
   "cupoMaximo": 0,
  "ubicacion": "",
  "especialidad": ""
}
```
>- Actualizar datos
###
## 🏥 RESTAURANTES
>`GET`
>
>`/api/restaurante/all`
>- Lista todos los restaurantes
 ###
>`GET`
>
>`/api/restaurante/especialidad?especialidadDeComida=PASTAS`
>- Filtra por especialidad
###
>`GET`
>
>`/api/restaurante/{id}`
>- Buscar restaurante por ID
###
>`DELETE`
>
>`/api/restaurante/{id}/delete`
>
> - Eliminar restaurante
###
>`PATCH`

```
/api/restaurante/actualizar/{id}

> Body
{
  "nombre": "",
   "cupoMaximo": 0,
  "ubicacion": "",
  "especialidad": ""
}
```
>- Actualizar datos
###
## 🍽️ CARTAS
>`POST`
```
/api/carta/subir

> form-data
archivo type=file (y se sube un archivo pdf)
id      type=text (id del restaurante)
```
>- Subir carta (PDF)
###
>`GET`
>
>`/api/carta/{id}/descargar`
>
>- Ver o descargar carta
###
>`PUT`
```
/api/carta/actualizar

> form-data
  archivo type=file (y se sube un archivo pdf)
  id      type=text (id del restaurante)
```
>- Actualizar carta PDF
###
>`DELETE`
>
>`/api/carta/{id}`
>
>- Eliminar carta asociada
## 📕 RESERVAS

>`POST`

```
/api/reserva/create

> Body
{
  "cantidad": 0,
  "estadoReserva": "",
  "fechaReserva": "",
  "horarioLlegada": "",
  "idCliente": 0,
  "idRestaurant": 0
}
```

>- Crear nueva reserva
###
>`GET`
>
>`/api/reserva/list`
>
>- Listar todas las reservas
###

>`GET`
>
>`/api/reserva/list_by_cliente/{id}`
>
>- Listar todas las reservas de un cliente
###

>`GET`
>
>`/api/reserva/list_by_restaurant/{id}`
>
>- Listar todas las reservas de un restaurant
###

>`GET`
>
>`/api/reserva/{id}`
>
>- Listar la reseña indicada
###

>`PUT`

```
/api/reserva/{id}/

> Body
{
  "id": 0,
  "comentario": "",
  "calificacion": 0
}
```

>- Crear nueva reserva
###

>`DELETE`
>
>/api/reserva/{id}
>
>- Eliminar reserva por ID

## 📄 RESEÑAS

>`POST`
```
>>/api/resena/create

> Body
{
  "id": 0,
  "comentario": "",
  "calificacion": 0,
  "clienteId": 0,
  "restaurantId": 0
}
```
>- Crear nueva reseña
###
>`GET`
>
>`/api/resena/{id}/allXcliente`
>
>- Listar todas las reseñas de un usuario
###
>`GET`
>
>`/api/resena/{id}`
>
>- Listar la reseña solicitada
###
>`GET`
>
>`/api/resena/list`
>
>- Listar todas las reseñas
###
>`PUT`
>
>`api/resena/{id}/actualizar`
>
> Actualizar reseña
###
>`DELETE`
>
>/api/resena/{id}/eliminar
>
> Eliminar reseña
###
## 🔑 AUTENTICACIÓN

>`POST`
```
/api/auth/login

> Body
{
  "email": "",
  "password": ""
}
```
>- Login y obtención de token JWT
###
>`POST`
```
/api/auth/register/cliente
> Body
{
  "nombre": "",
  "apellido": "",
  "email": "",
  "password": "",
  "telefono": ""
}

```
>- Registro de nuevo cliente
###
>`POST`
```
/api/auth/register/restaurant

> Body
{
  "email": "",
  "password": "",
  "nombreRestaurante": "",
  "direccion": "",
  "telefono": "",
  "especialidadDeComida": "",
  "cupoMaximo": 0
}
```
>- Registro de restaurante (encargado)

## 👮 ADMINISTRADOR

>`PUT`
>
>/api/admin/{usuarioId}/aprobar
>
>- Aprobar cuenta de encargado
###

>`GET`
>
>
>- Lista todos los restaurantes por aprobar
###
## 🎡 Roles y Permisos

### CLIENTE

- Crear reservas

- Crear reseñas

### ENCARGADO

- Subir carta PDF

- Modificar restaurante propio

### ADMIN

- Aprobar encargados

- Acceso completo

```
> Cuenta de admin

{
  "email":"admin@foodies.com,
  "password":"admin123",
}
```

<h2>📄 Datos de Prueba (Postman)</h2>

Registro de Cliente

```
> POST /api/auth/register/cliente
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@gmail.com",
  "password": "1234",
  "telefono": "11334455"
}
```
Login

```
> POST /api/auth/login
{
  "email": "juan@gmail.com",
  "password": "1234"
}
```
`Respuesta: Bearer <token>`

<h3>📢 Autor</h3>

Desarrollado por Andres Roma, Ramiro Sacchetta y Juan Estavillo como parte del trabajo final de la materia.

<h3>🚧 Licencia</h3>

Este proyecto es de uso educativo y libre distribución para fines académicos.
