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
>`/api/clientes/`
>- Lista todos los clientes
###
>`GET`
>
>`/api/clientes/{id}`
>- Lista al cliente solicitado
###
>`DELETE`
>
>`/api/clientes/{id}`
>- Elimina al cliente solicitado
###
>`PATCH`

```
/api/clientes/{id}

> Body
{
  "nombre": "",
  "apellido": "",
  "telefono":""
}
```
>- Actualizar datos
###
## 🏥 RESTAURANTES
>`GET`
>
>`/api/restaurantes`
>- Lista todos los restaurantes
 ###
>`GET`
>
>`/api/restaurantes/especialidad?especialidadDeComida=PASTAS`
>- Filtra por especialidad
###
>`GET`
>
>`/api/restaurantes/{id}`
>- Buscar restaurante por ID
###
>`DELETE`
>
>`/api/restaurantes/{id}`
>
> - Eliminar restaurante (solo `ROLE_ADMIN`; también borra el PDF de su carta)
###
>`PATCH` (solo `ROLE_ADMIN`; el encargado usa `PATCH /api/restaurantes/me`)

```
/api/restaurantes/{id}

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
## 🧑‍🍳 PANEL DEL ENCARGADO (`ROLE_ENCARGADO`)
Todos los endpoints trabajan sobre el restaurante del usuario logueado (se resuelve por el email del token), así un encargado nunca puede tocar el restaurante de otro.

>`GET` `/api/restaurantes/me`
>- Datos del restaurante y su carta (`carta` es `null` si no subió ninguna)
###
>`PATCH` `/api/restaurantes/me`
```
> Body (solo se cambian los campos enviados)
{
  "nombre": "",
  "cupoMaximo": 1,
  "ubicacion": "",
  "especialidad": "PARRILLA"
}
```
###
>`PUT` `/api/restaurantes/me/carta`
```
> form-data
archivo type=file (PDF, máximo 10 MB)
```
>- Sube o reemplaza la carta. Se valida que el contenido sea un PDF real. Al reemplazar, el archivo anterior se borra.
###
>`DELETE` `/api/restaurantes/me/carta`
>- Elimina la carta y su archivo
###
>`GET` `/api/restaurantes/me/reservas`
>- Reservas del restaurante ordenadas por fecha y hora (`[]` si no hay)
###
>`PATCH` `/api/reservas/{id}/estado`
```
> Body
{ "estado": "ACEPTADA" }   // o "CANCELADA"
```
>- Solo para reservas del propio restaurante (si no, `403`)
###
## 🍽️ CARTAS
Los PDF ya no se guardan en la base de datos: se guardan en un almacenamiento de archivos (Cloudflare R2 en producción, disco local en desarrollo) y la tabla `cartas` solo guarda la clave del archivo.

>`GET` `/api/restaurantes/{id}/carta` (público)
>- Devuelve `{ id, nombreArchivo, url, tamanioBytes, actualizadaEn }`
###
>`GET` `/api/carta/{id}` (público)
>- Redirige (`302`) a la URL del PDF. Se mantiene por compatibilidad.
###
>`DELETE` `/api/carta/{id}` (solo `ROLE_ADMIN`)
>- Elimina la carta y su archivo

### Almacenamiento de cartas
| Variable | Para qué |
|---|---|
| `STORAGE_PROVIDER` | `local` (por defecto) o `r2` |
| `STORAGE_LOCAL_DIR` | Carpeta para `local` (por defecto `uploads/`, ignorada por git). Los archivos se sirven en `/api/files/...` |
| `R2_ACCOUNT_ID` | Account ID de Cloudflare |
| `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` | Token de API de R2 con permiso de lectura y escritura sobre el bucket |
| `R2_BUCKET` | Nombre del bucket |
| `R2_PUBLIC_BASE_URL` | URL pública del bucket (`https://pub-xxxx.r2.dev` o un dominio propio), sin `/` final |

Con `STORAGE_PROVIDER=r2`, si falta alguna variable la app no arranca y el error dice cuáles faltan.
## 📕 RESERVAS

>`POST`

```
/api/reservas

> Body
{
  "cantidad": 0,
  "estadoReserva": "",
  "fechaReserva": "",
  "horarioLlegada": "",
  "idUsuario": 0,
  "idRestaurant": 0
}
```

>- Crear nueva reserva
###
>`GET`
>
>`/api/reservas`
>
>- Listar todas las reservas
###

>`GET`
>
>`/api/reservas/usuario/{id}`
>
>- Listar todas las reservas de un usuario
###

>`GET`
>
>`/api/reservas/restaurante/{id}`
>
>- Listar todas las reservas de un restaurant
###

>`GET`
>
>`/api/reservas/{id}`
>
>- Listar la reseña indicada
###

>`PUT`

```
/api/reservas/{id}/

> Body
{
  "cantidad": ,
  "estadoReserva: ""
}
```

>- Crear nueva reserva
###

>`DELETE`
>
>/api/reservas/{id}
>
>- Eliminar reserva por ID

## 📄 RESEÑAS

>`POST`
```
>>/api/resenas

> Body
{
  "id": 0,
  "comentario": "",
  "calificacion": 0,
  "usuarioId": 0,
  "restaurantId": 0
}
```
>- Crear nueva reseña
###
>`GET`
>
>`/api/resenas/usuario/{id}`
>
>- Listar todas las reseñas de un usuario
###
>`GET`
>
>`/api/resenas/{id}`
>
>- Listar la reseña solicitada
###
>`GET`
>
>`/api/resenas`
>
>- Listar todas las reseñas
###
>`PUT`
```
>>/api/resenas/{id}

> Body
{
  "id": 0,
  "comentario": "",
  "calificacion": 0
}
```
>- Actualizar reseña
> Actualizar reseña
###
>`DELETE`
>
>/api/resenas/{id}
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
/api/auth/register/restaurante

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
>`/api/admin/approved/{usuarioId}`
>
>- Aprobar cuenta de encargado
###

>`GET`
>
>`/api/admin/requests`
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
