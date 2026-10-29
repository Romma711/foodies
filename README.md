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

<h4>📘 Documentación interactiva (Swagger)</h4>

Con el backend levantado:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Especificación OpenAPI (JSON): http://localhost:8080/v3/api-docs

Para probar endpoints protegidos, hacé login en `POST /api/auth/login`, copiá el token y pegalo en el botón **Authorize** (sin el prefijo `Bearer`).

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

>`GET`
>
>`/api/admin/metricas`
>
>- Métricas del dashboard: top 5 restaurantes mejor calificados (solo aprobados), top 5 con más reservas (sin canceladas), top 5 usuarios con más reseñas y usuarios registrados en el mes en curso
```json
{
  "periodo": "2026-10",
  "topRestaurantesMejorCalificados": [{ "id": 5, "nombre": "Ochiwan", "promedio": 4.5, "cantidadResenas": 2 }],
  "topRestaurantesConMasReservas": [{ "id": 5, "nombre": "Ochiwan", "cantidadReservas": 7 }],
  "topUsuariosConMasResenas": [{ "id": 3, "nombre": "Ana", "apellido": "Gómez", "cantidadResenas": 4 }],
  "usuariosRegistradosEsteMes": 12
}
```
###

>`GET`
>
>`/api/admin/resenas?limite=20`
>
>- Reseñas más recientes de todos los restaurantes para moderar (límite entre 1 y 50, por defecto 20). Para eliminar se usa `DELETE /api/resenas/{id}`
```json
[{ "id": 9, "comentario": "Muy rico", "calificacion": 5, "nombreUsuario": "Ana Gómez", "restauranteId": 5, "nombreRestaurante": "Ochiwan" }]
```
###

### Moderación (Fase 3)

Los restaurantes tienen `estado` (`PENDIENTE`, `APROBADO`, `RECHAZADO`), sincronizado con `aprobado`: un registro nuevo arranca en `PENDIENTE` y `PUT /api/admin/approved/{usuarioId}` también lo pasa a `APROBADO`. Las filas anteriores a la columna se completan al arrancar a partir de `aprobado`. Restaurantes y usuarios suman `strikes` (default 0) y `activo` (default `true`, para la baja lógica).

>`GET`
>
>`/api/admin/restaurantes/pendientes`
>
>- Solicitudes de alta en estado `PENDIENTE`, con los datos de contacto del encargado
```json
[{ "restaurantId": 7, "nombre": "Pasta Nostra", "ubicacion": "Av. Colón 1200", "especialidad": "PASTAS", "cupoMaximo": 40, "estado": "PENDIENTE", "usuarioId": 12, "email": "pasta@mail.com", "telefono": "2234000000", "fechaRegistro": "2026-10-28T10:15:00" }]
```
###

>`PUT`
>
>`/api/admin/restaurantes/{id}/aprobar` · `/api/admin/restaurantes/{id}/rechazar`
>
>- Aprueba (publica el restaurante y le da `ROLE_ENCARGADO` a su cuenta) o rechaza una solicitud. Rechazar un restaurante ya aprobado responde 400
```json
{ "restaurantId": 7, "nombre": "Pasta Nostra", "estado": "APROBADO" }
```
###

>`POST`
>
>`/api/admin/usuarios/{id}/strikes` · `/api/admin/restaurantes/{id}/strikes`
>
>- Suma un strike y envía el aviso por email (al usuario o al encargado del restaurante). `motivo` es obligatorio (5 a 255 caracteres). A los administradores no se les puede dar strikes. Si el email falla, el strike igual se guarda y `emailEnviado` vuelve en `false`. Con 3 strikes, `sugerirBaja` es `true`. Limitación conocida: el contador se incrementa sin bloqueo, así que dos strikes simultáneos sobre la misma cuenta pueden contarse como uno; es aceptable porque la moderación la hace un solo administrador
```json
{ "motivo": "Lenguaje ofensivo en una reseña" }
```
```json
{ "id": 4, "nombre": "Ana Gómez", "email": "ana@mail.com", "strikes": 3, "sugerirBaja": true, "emailEnviado": true }
```
###

>`PUT`
>
>`/api/admin/restaurantes/{id}/ubicacion`
>
>- Carga las coordenadas para el mapa (latitud entre -90 y 90, longitud entre -180 y 180). `GET /api/restaurantes` y `GET /api/restaurantes/{id}` ahora devuelven `latitud` y `longitud` (`null` hasta que se cargan)
```json
{ "latitud": -38.0055, "longitud": -57.5426 }
```
###

>`GET`
>
>`/api/admin/entidades/clientes` · `/api/admin/entidades/restaurantes`
>
>- Listados para "Gestión de Entidades": clientes con `strikes` y `activo`; restaurantes en cualquier estado con `estado`, `strikes`, `activo` y el email del encargado
```json
[{ "id": 7, "nombre": "Pasta Nostra", "ubicacion": "Av. Colón 1200", "especialidad": "PASTAS", "estado": "APROBADO", "strikes": 1, "activo": true, "usuarioId": 12, "email": "pasta@mail.com" }]
```
###

>`PATCH`
>
>`/api/admin/usuarios/{id}/activo` · `/api/admin/restaurantes/{id}/activo`
>
>- Baja lógica (`false`) o reactivación (`true`); no borra nada. Un usuario dado de baja recibe 403 `cuenta dada de baja` al intentar loguearse (con cualquier contraseña) y en cualquier request con un token anterior. Las consultas públicas (`GET /api/restaurantes`, `/especialidad` y `/{id}`) solo muestran restaurantes aprobados y activos a la vez; el detalle de uno pendiente, rechazado o dado de baja responde 404. Un restaurante dado de baja tampoco acepta reservas nuevas (`POST /api/reservas` responde 404 `Restaurante no encontrado`). A un administrador no se lo puede dar de baja (400)
```json
{ "activo": false }
```
###

### Emails

| Cuándo | Para quién | Asunto |
|---|---|---|
| El restaurante acepta una reserva (`PATCH /api/reservas/{id}/estado` con `ACEPTADA`) | Cliente | `Foodies: tu reserva en {restaurante} está confirmada` (fecha, hora, personas y dirección) |
| El admin aprueba un restaurante (`PUT /api/admin/restaurantes/{id}/aprobar`) | Encargado | `Foodies: tu restaurante {restaurante} fue aprobado` |
| El admin aplica un strike | Usuario o encargado | `Foodies: aviso de sanción (n de 3)` |

Los avisos de reserva confirmada y de restaurante aprobado salen después de guardar el cambio y no se repiten si la reserva ya estaba aceptada o el restaurante ya estaba aprobado. Si el correo falla, la operación igual queda hecha y el error queda en el log.

Se envían con `JavaMailSender`. Por defecto apuntan al sandbox de [Mailtrap](https://mailtrap.io): cargá las credenciales de tu inbox en estas variables de entorno. Para usar un SMTP real alcanza con cambiarlas.

| Variable | Default |
|---|---|
| `MAIL_HOST` | `sandbox.smtp.mailtrap.io` |
| `MAIL_PORT` | `2525` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | vacías (las da Mailtrap) |
| `MAIL_FROM` | `no-reply@foodies.com` |

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

<h2>🗄️ Base de datos local y datos de demo</h2>

- `docs/db/crear-usuario-local.sql`: crea la base `foodies` y el usuario `foodies_app` (correr una vez como root).
- `docs/db/seed-demo.mjs`: con el backend levantado, `node docs/db/seed-demo.mjs` carga 6 restaurantes aprobados, 8 clientes, reseñas y reservas (algunas canceladas) para ver el dashboard del admin. Todas las cuentas de demo usan la contraseña `demo1234`.

<h3>📢 Autor</h3>

Desarrollado por Andres Roma, Ramiro Sacchetta y Juan Estavillo como parte del trabajo final de la materia.

<h3>🚧 Licencia</h3>

Este proyecto es de uso educativo y libre distribución para fines académicos.
