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
- Definir la clave para firmar los tokens JWT en la variable de entorno `JWT_SECRET` (al menos 32 bytes; por ejemplo, la salida de `openssl rand -base64 48`). No hay clave por defecto: si falta o es más corta, la app no arranca y el error lo indica. Los tests usan una clave propia definida en `src/test/resources/application.properties`.
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
>`/api/clientes`
>- Lista todos los clientes (solo `ROLE_ADMIN`)
###
>`GET`
>
>`/api/clientes/{id}`
>- Lista al cliente solicitado (el admin o el propio cliente; otro id responde `403`)
###
>`DELETE`
>
>`/api/clientes/{id}`
>- El propio cliente: da de baja su cuenta (baja lógica, `204`). Sus reservas futuras activas se cancelan y se le avisa a cada restaurante; el historial se conserva y la cuenta ya no puede iniciar sesión
>- El admin: borra la cuenta solo si no tiene reservas ni reseñas; si las tiene responde `409` ("dalo de baja en lugar de borrarlo")
>- Otro id responde `403`
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
>- Actualizar datos (el admin o el propio cliente; otro id responde `403`). No permite cambiar el rol ni el estado de la cuenta.
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
>`GET` (público)
>
>`/api/restaurantes/{id}/espacios?fecha=2026-12-24&cantidad=4`
>- Espacios activos donde se puede reservar: `[{ "id", "tipo": "SALON" | "PATIO" | "BARRA", "nombre", "hayLugar" }]`
>- `fecha` y `cantidad` son opcionales pero van juntos (si llega uno solo, `400`). Con ellos, `hayLugar` dice si entran esas personas ese día en ese espacio; sin ellos es `null`. Nunca se muestra la capacidad
>- Restaurante pendiente, rechazado o dado de baja: `404`
###
>`DELETE`
>
>`/api/restaurantes/{id}`
>
> - Eliminar restaurante (solo `ROLE_ADMIN`; también borra el PDF de su carta)
> - Con reservas activas a futuro responde `409`: primero hay que darlo de baja, eso las cancela y avisa a los clientes
###
>`PATCH` (solo `ROLE_ADMIN`; el encargado usa `PATCH /api/restaurantes/me`)

```
/api/restaurantes/{id}

> Body
{
  "nombre": "",
  "ubicacion": "",
  "especialidad": ""
}
```
>- Actualizar datos
>- `cupoMaximo` ya no se edita: es la suma de los espacios activos. Si llega, responde `400`
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
  "ubicacion": "",
  "especialidad": "PARRILLA",
  "porcentajeAutoAceptacion": 60
}
```
>- `porcentajeAutoAceptacion` (0-100): las reservas se aceptan solas mientras las personas aceptadas del día en ese espacio no superen ese porcentaje de su cupo. `0` apaga la auto-aceptación
>- `cupoMaximo` (capacidad total) es de solo lectura: se calcula con los espacios. Si llega, responde `400`
###
### Espacios
Cada restaurante tiene uno o más espacios (salones, patios, barras) con su capacidad por día. Al registrarse se crea "Salón principal" con la capacidad indicada. El cliente elige el espacio al reservar y el cupo se valida en ese espacio.

>`GET` `/api/restaurantes/me/espacios`
>- Todos los espacios, activos e inactivos: `[{ "id", "tipo", "nombre", "capacidad", "activo" }]`
###
>`POST` `/api/restaurantes/me/espacios` (`201`) y `PUT` `/api/restaurantes/me/espacios/{id}`
```
> Body
{ "tipo": "PATIO", "nombre": "Patio trasero", "capacidad": 20 }
```
>- `tipo`: `SALON`, `PATIO` o `BARRA`. `nombre` hasta 60 caracteres y sin repetir dentro del restaurante. `capacidad` al menos 1
>- La capacidad no puede quedar por debajo de las personas ya reservadas en un mismo día futuro (`400`)
>- Al bajar la capacidad, los cupos especiales futuros mayores que la nueva capacidad pasan a ser iguales a ella
>- Un espacio de otro restaurante responde `404`
###
>`PATCH` `/api/restaurantes/me/espacios/{id}/activo`
```
> Body
{ "activo": false }
```
>- Los espacios no se borran: se desactivan. No se puede desactivar el último espacio activo ni uno con reservas pendientes o aceptadas para los próximos días (`400`)
###
### Horario de atención
Franjas por día de la semana en las que el restaurante recibe llegadas. Sin horario cargado no hay restricción.

>`GET` `/api/restaurantes/me/horarios` y `GET` `/api/restaurantes/{id}/horarios` (público)
>- `[{ "dia": "SATURDAY", "apertura": "12:00", "cierre": "16:00" }]`, ordenadas por día y apertura
###
>`PUT` `/api/restaurantes/me/horarios`
```
> Body (reemplaza la semana completa; [] quita el horario)
[
  { "dia": "SATURDAY", "apertura": "12:00", "cierre": "16:00" },
  { "dia": "SATURDAY", "apertura": "20:00", "cierre": "00:00" }
]
```
>- `dia`: `MONDAY` … `SUNDAY`. Horas `HH:mm`; `cierre` `00:00` es medianoche. Hasta 3 franjas por día, sin superponerse (`400`)
>- Al crear o modificar una reserva, la hora de llegada tiene que caer en una franja de ese día (desde la apertura y antes del cierre). Si no: `400` "Ese día el restaurante está cerrado…" o "Ese día el restaurante recibe reservas de 20:00 a 00:00…"
###
>`GET` `/api/restaurantes/me/cupos-especiales`
>- Próximos cupos especiales de todos los espacios: `[{ "fecha", "cupo", "motivo", "reservadas", "idEspacio", "espacioNombre" }]`
###
>`PUT` `/api/restaurantes/me/espacios/{espacioId}/cupos-especiales/{fecha}`
```
> Body
{ "cupo": 0, "motivo": "Patio cerrado por lluvia" }
```
>- Ese día el espacio usa este cupo en lugar de su capacidad. Entre 0 y la capacidad del espacio, no en días pasados, y no menos que lo ya reservado ese día en ese espacio (`400`)
###
>`DELETE` `/api/restaurantes/me/espacios/{espacioId}/cupos-especiales/{fecha}`
>- Ese día el espacio vuelve a su capacidad normal (`204`)
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
>- Reservas del restaurante ordenadas por fecha y hora (`[]` si no hay), con `idEspacio` y `espacioNombre`
###
>`PATCH` `/api/reservas/{id}/estado`
```
> Body
{ "estado": "ACEPTADA" }   // ACEPTADA, RECHAZADA, CANCELADA, ASISTIO o NO_ASISTIO
```
>- Solo para reservas del propio restaurante (si no, `403`). Con el restaurante dado de baja, `403` (panel en modo lectura)
>- Antes de la hora de llegada: `PENDIENTE` → `ACEPTADA` (vuelve a validar el cupo) o `RECHAZADA`; `PENDIENTE` o `ACEPTADA` → `CANCELADA`. Después de la llegada solo se marca la asistencia
>- Desde la hora de llegada y hasta 48 horas después: `ACEPTADA` → `ASISTIO` o `NO_ASISTIO`. Cada 3 `NO_ASISTIO` el cliente suma un strike automático
>- Los estados finales (`CANCELADA`, `RECHAZADA`, `ASISTIO`, `NO_ASISTIO`, `VENCIDA`) no cambian más. Cualquier otro cambio responde `400` con el motivo
>- Rechazar o cancelar le avisa al cliente por mail y le ofrece el lugar a la lista de espera
###
### Turnos (RF24)
>`GET` `/api/restaurantes/me/turnos`
>- Turnos del restaurante: `[{ "id", "dia", "inicio", "fin", "duracionMinutos" }]`
###
>`POST` `/api/restaurantes/me/turnos` · `PUT` `/api/restaurantes/me/turnos/{id}`
```
> Body
{ "dia": "SATURDAY", "inicio": "20:00", "duracionMinutos": 90 }   // duracionMinutos opcional (90)
```
>- El cupo de cada espacio rige por turno y la hora de llegada es el inicio del turno. Un restaurante sin turnos sigue con cupo por día
>- Duración entre 15 y 360 minutos, terminando a la medianoche como máximo; con horario cargado, el turno tiene que empezar y terminar dentro de una franja de ese día (`400`). Dos turnos el mismo día a la misma hora: `409`
>- `POST` responde `201`
###
>`DELETE` `/api/restaurantes/me/turnos/{id}`
>- Quita el turno (`204`). Un turno con reservas activas a futuro no se cambia ni se borra (`409`)
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
  "idRestaurant": 0,
  "idEspacio": 0
}
```

>- Crear nueva reserva. Solo `ROLE_CLIENTE` (otro rol: `403`). La reserva queda a nombre del usuario del token: `idUsuario` es opcional y, si es de otro usuario, responde `403`
>- `idEspacio` es obligatorio: un espacio activo de ese restaurante (si no, `400`). El cupo se valida en ese espacio (su cupo especial o su capacidad) por día, o por turno si el restaurante tiene turnos, sin revelar la capacidad
>- `idTurno` es opcional: con turnos, si no se envía se usa el turno que empieza en `horarioLlegada`; si ninguno coincide responde `400` con los turnos de ese día
>- La fecha y hora no pueden haber pasado (`400`); la última llegada es 60 minutos antes del cierre (con turnos, el turno tiene que terminar antes del cierre)
>- Un cliente tiene como máximo una reserva activa (`PENDIENTE` o `ACEPTADA`) por restaurante y día, y dos reservas activas en restaurantes distintos tienen que estar a 2 horas o más (`409`)
>- Con 3 strikes el cliente puede entrar y ver sus reservas pero no reservar (`403`)
>- Se avisa por mail al restaurante. Reservar cierra la inscripción del cliente en la lista de espera de ese restaurante y día
>- Las respuestas de reservas incluyen `idEspacio`, `espacioNombre` e `idTurno` (`null` sin turnos)
###
>`GET`
>
>`/api/reservas`
>
>- Listar todas las reservas (solo `ROLE_ADMIN`)
###

>`GET`
>
>`/api/reservas/usuario/{id}`
>
>- Listar todas las reservas de un usuario (el propio usuario o el admin; si no, `403`)
###

>`GET`
>
>`/api/reservas/restaurante/{id}`
>
>- Listar todas las reservas de un restaurant (su encargado o el admin; si no, `403`)
###

>`GET`
>
>`/api/reservas/{id}`
>
>- Ver la reserva indicada (su dueño, el encargado del restaurante o el admin; si no, `403`)
###

>`PUT`

```
/api/reservas/{id}

> Body (todos opcionales)
{
  "cantidad": 4,
  "fechaReserva": "2026-12-24",
  "horarioLlegada": "21:30",
  "idEspacio": 3,
  "idTurno": 7,
  "estadoReserva": "CANCELADA"
}
```

>- Solo el dueño de la reserva (si no, `403`)
>- Con `estadoReserva` solo se acepta `CANCELADA` (cancela la reserva e ignora los demás campos); otro valor responde `400`
>- Solo hasta 30 minutos antes de la llegada (después, `400`: los cambios de último momento se hablan con el restaurante)
>- Si cambia fecha, hora, turno, cantidad o espacio, se vuelve a evaluar la auto-aceptación: queda `ACEPTADA` si la cubre, si no vuelve a `PENDIENTE`. Se valida: cantidad mayor a 0, fecha y hora no anteriores a ahora (y hasta 3 meses), duplicados (`409`) y cupo del día o del turno en el espacio (el nuevo, si cambia) sin contar esta misma reserva. Los errores responden `400` con `{ "error", "mensaje" }`
>- Cancelar le avisa al restaurante por mail y le ofrece el lugar a la lista de espera
>- Una reserva `CANCELADA` o en otro estado final no se puede modificar (`400`)
###

>`DELETE`
>
>/api/reservas/{id}
>
>- Eliminar reserva por ID (solo el admin). El cliente recibe `403`: las reservas se cancelan, no se borran
###
### Estados y reglas de una reserva
>- `PENDIENTE` → `ACEPTADA` / `RECHAZADA` / `CANCELADA`; `ACEPTADA` → `CANCELADA` / `ASISTIO` / `NO_ASISTIO`
>- Una reserva `PENDIENTE` que nadie aceptó antes de su hora de llegada pasa a `VENCIDA` (tarea programada cada 5 minutos)
>- Solo `PENDIENTE` y `ACEPTADA` ocupan cupo y cuentan como reserva activa
>- Dar de baja un cliente, un restaurante o su encargado cancela sus reservas futuras activas y le avisa a la otra parte por mail. Un restaurante dado de baja (o con su encargado dado de baja) sale del catálogo y su panel queda en modo lectura
>- Los errores de estas reglas suman `codigo` (estable, por ejemplo `RESERVA_DUPLICADA`, `PLAZO_VENCIDO`, `SIN_CUPO_TURNO`) y `params` (los valores del mensaje, por ejemplo `{ "minutos": 30 }`) además de `error` y `mensaje`, para que el front los traduzca. Los errores sin código responden como siempre
>- Mails: reserva nueva → restaurante; aceptada, rechazada o cancelada por el restaurante → cliente; cancelada por el cliente → restaurante; bajas → parte afectada; lugar libre → primero de la lista de espera
###
>`GET` `/api/restaurantes/{id}/turnos`
>- Turnos del restaurante (público)
###
>`GET` `/api/restaurantes/{id}/disponibilidad?fecha=yyyy-MM-dd`
>- Lugares libres por turno y espacio (público). Con turnos: `{ "fecha", "porTurnos": true, "turnos": [{ "idTurno", "inicio", "fin", "espacios": [{ "idEspacio", "nombre", "tipo", "libres" }] }] }` (solo los turnos que todavía no empezaron). Sin turnos: un único bloque del día con `idTurno`, `inicio` y `fin` en `null`
###
## ⏳ LISTA DE ESPERA (RF25, `ROLE_CLIENTE`)
>`POST` `/api/lista-espera`
```
> Body
{ "idRestaurant": 1, "fechaReserva": "2026-12-24", "horarioLlegada": "21:30", "idTurno": null, "cantidad": 2 }
```
>- Solo si no hay lugar en ningún espacio para esa fecha y horario o turno (si hay, `409` "Hay lugar en ese horario: reservá directamente"). Una inscripción abierta por cliente, restaurante y día (`409`). Con 3 strikes, `403`
>- Responde `201` con `{ "id", "idRestaurant", "nombreResto", "fecha", "horario", "idTurno", "cantidad", "estado", "creada" }`
>- Cuando se libera lugar (cancelación, rechazo o baja) se le avisa por mail a la primera inscripción que entra y queda `NOTIFICADA`. El lugar no se guarda: el cliente reserva como siempre. Las inscripciones vencen al pasar su horario (`VENCIDA`)
###
>`GET` `/api/lista-espera/mias`
>- Inscripciones del cliente (sin las que dejó), de la más nueva a la más vieja
###
>`DELETE` `/api/lista-espera/{id}`
>- Salir de la lista (`204`). Una inscripción de otro cliente responde `403`

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
>- Solo quien asistió: hace falta una reserva marcada `ASISTIO` por ese restaurante (si no, `403`). El restaurante tiene que estar publicado (aprobado y activo; si no, `404`). Una reseña por restaurante
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
>`/api/resenas?id={restauranteId}`
>
>- Listar las reseñas de un restaurante, de la más nueva a la más vieja según `fechaCreacion` (ISO `yyyy-MM-ddTHH:mm:ss`). Las reseñas anteriores a ese campo tienen `fechaCreacion: null` y van al final
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
>`/api/admin/resenas?limite=20&pagina=0&texto=sushi&calificacion=1&restauranteId=5`
>
>- Reseñas más recientes de todos los restaurantes para moderar (límite entre 1 y 50, por defecto 20). Para eliminar se usa `DELETE /api/resenas/{id}`
>- Filtros opcionales: `texto` busca en el comentario, el autor y el restaurante sin distinguir mayúsculas; `calificacion` exacta (1 a 5); `restauranteId`. `pagina` (desde 0) sirve para "Cargar más". Calificación fuera de 1-5 o página negativa: `400`
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
[{ "restaurantId": 7, "nombre": "Pasta Nostra", "ubicacion": "Av. Colón 1200", "especialidad": "PASTAS", "cupoMaximo": 40, "estado": "PENDIENTE", "usuarioId": 12, "email": "pasta@mail.com", "telefono": "2234000000", "fechaRegistro": "2026-10-28T10:15:00", "fotos": [{ "id": 1, "url": "/api/files/fotos/7/portada.jpg", "portada": true }] }]
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
>- Suma un strike y envía el aviso por email (al usuario o al encargado del restaurante). `motivo` es obligatorio (5 a 255 caracteres). A los administradores no se les puede dar strikes. Si el email falla, el strike igual se guarda y `emailEnviado` vuelve en `false`. Con 3 strikes, `sugerirBaja` es `true` y no se admiten más (400): el siguiente paso es la baja. Limitación conocida: el contador se incrementa sin bloqueo, así que dos strikes simultáneos sobre la misma cuenta pueden contarse como uno; es aceptable porque la moderación la hace un solo administrador. No se puede sancionar una cuenta o restaurante dado de baja (400). Con 3 strikes el cliente sigue entrando y viendo sus reservas, pero no puede reservar ni anotarse en listas de espera
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
>- Baja lógica (`false`) o reactivación (`true`); no borra nada. Un usuario dado de baja recibe 403 `cuenta dada de baja` al intentar loguearse (con cualquier contraseña) y en cualquier request con un token anterior. Las consultas públicas (`GET /api/restaurantes`, `/especialidad` y `/{id}`) solo muestran restaurantes aprobados y activos a la vez; el detalle de uno pendiente, rechazado o dado de baja responde 404. Un restaurante dado de baja tampoco acepta reservas nuevas (`POST /api/reservas` responde 404 `Restaurante no encontrado`). A un administrador no se lo puede dar de baja (400). Dar de baja cancela las reservas futuras activas del cliente, del restaurante o del restaurante de ese encargado y avisa por mail a la otra parte; un restaurante cuyo encargado fue dado de baja también sale del catálogo, y el panel de un restaurante dado de baja solo permite lecturas (`403` en cualquier escritura)
```json
{ "activo": false }
```
###

### Emails

| Cuándo | Para quién | Asunto |
|---|---|---|
| El restaurante acepta una reserva (`PATCH /api/reservas/{id}/estado` con `ACEPTADA`) o la reserva se crea ya aceptada por la aceptación automática | Cliente | `Foodies: tu reserva en {restaurante} está confirmada` (fecha, hora, personas y dirección) |
| El admin aprueba un restaurante (`PUT /api/admin/restaurantes/{id}/aprobar`) | Encargado | `Foodies: tu restaurante {restaurante} fue aprobado` |
| El admin aplica un strike | Usuario o encargado | `Foodies: aviso de sanción (n de 3)` |
| El restaurante marca la tercera ausencia (`NO_ASISTIO`) | Cliente | `Foodies: aviso de sanción (n de 3)` (strike automático) |
| Un cliente reserva | Restaurante | `Foodies: nueva reserva en {restaurante}` |
| El restaurante rechaza una reserva | Cliente | `Foodies: tu reserva en {restaurante} no fue confirmada` |
| El restaurante cancela una reserva | Cliente | `Foodies: {restaurante} canceló tu reserva` |
| El cliente cancela, o su cuenta se da de baja | Restaurante | `Foodies: se canceló una reserva en {restaurante}` |
| El restaurante o su encargado se dan de baja | Cliente | `Foodies: tu reserva en {restaurante} fue cancelada` |
| Se libera lugar en una fecha con lista de espera | Primer cliente anotado que entra | `Foodies: se liberó un lugar en {restaurante}` |

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
