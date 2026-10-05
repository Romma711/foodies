# 🍽️ Foodies — Sistema de reservas de restaurantes

API REST para gestionar restaurantes, cartas en PDF, reservas y reseñas, con
autenticación JWT y permisos por rol.

**Integrantes:** Ramiro Sacchetta · Andres Roma · Juan Estavillo · **Gonzalo Leonel Lopez**

---

## 🧰 Stack

Java 21 · Spring Boot 3.4 · Spring Web · Spring Data JPA · Spring Security ·
Spring Validation · MapStruct · Lombok · MySQL · JWT · Maven

---

## 🚀 Cómo correrlo

**Necesitás:** Java 21, Maven y MySQL.

**1. Crear la base**

```sql
CREATE DATABASE foodies;
```

**2. Configurar las variables de entorno**

La app no tiene secretos en el código: todo sale de variables de entorno.

| Variable | Para qué es |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Servidor, puerto y nombre de la base |
| `DB_USER`, `DB_PASS` | Credenciales de MySQL |
| `ADMIN_PASSWORD` | Contraseña de la cuenta admin que se crea al arrancar (mín. 8 caracteres) |
| `JWT_SECRET` | Clave de firma de los tokens (mín. 32 bytes) |

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=foodies
export DB_USER=root
export DB_PASS=tu-clave
export ADMIN_PASSWORD=admin123
export JWT_SECRET=una-clave-larga-de-32-bytes-o-mas
```

> En la rama `fix/flujo-principal` la app **no arranca** si faltan `JWT_SECRET` o
> `ADMIN_PASSWORD`: la configuración falla rápido en el startup.

**3. Correr**

```bash
./mvnw spring-boot:run
```

Queda disponible en `http://localhost:8080`.

Al arrancar se crea la cuenta admin `admin@foodies.com` con la contraseña de
`ADMIN_PASSWORD` (en `master` está fija en `admin123`).

---

## 🔑 Autenticación

Login → se devuelve un token JWT → se manda en `Authorization: Bearer <token>`.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"juan@gmail.com","password":"1234"}'
```

```
{ "Token": "Bearer eyJhbGciOi..." }
```

| Rol | Qué puede hacer |
|---|---|
| `CLIENTE` | Reservar, reseñar, ver su perfil y sus reservas |
| `ENCARGADO` | Lo anterior + manage su restaurante y subir la carta PDF |
| `ADMIN` | Todo: aprueba encargados, ve y edita cualquier recurso |

---

## 📡 Endpoints

### Autenticación

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/auth/register/cliente` | Registro de cliente |
| `POST` | `/api/auth/register/restaurante` | Registro de restaurante (queda `PENDIENTE` hasta que el admin lo apruebe) |
| `POST` | `/api/auth/login` | Login, devuelve el token |

### Restaurantes

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/restaurantes` | Restaurantes aprobados |
| `GET` | `/api/restaurantes/{id}` | Detalle (los no aprobados solo los ve su dueño o el admin) |
| `GET` | `/api/restaurantes/especialidad?especialidadDeComida=PASTAS` | Filtro por especialidad |
| `PATCH` | `/api/restaurantes/{id}` | Editar (encargado del local o admin) |
| `DELETE` | `/api/restaurantes/{id}` | Borrar (409 si tiene reservas o reseñas) |

Especialidades: `PESCADOS`, `PARRILLA`, `PASTAS`, `ASIATICA`, `MINUTAS`, `CAFE`.

### Cartas (PDF)

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/carta` | Sube la carta (`multipart`: `archivo` + `restaurantId`) |
| `PUT` | `/api/carta` | Reemplaza la carta (`multipart`: `archivo` + `restaurantId`) |
| `GET` | `/api/carta/{restaurantId}` | Ver / descargar el PDF (público) |
| `DELETE` | `/api/carta/{id}` | Borrar la carta |

### Reservas

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/reservas` | Crear reserva |
| `GET` | `/api/reservas` | Todas las reservas (admin) |
| `GET` | `/api/reservas/usuario/{id}` | Reservas de un cliente (él mismo o admin) |
| `GET` | `/api/reservas/restaurante/{id}` | Reservas de un restaurante (encargado o admin) |
| `GET` | `/api/reservas/{id}` | Detalle |
| `PUT` | `/api/reservas/{id}` | Editar cantidad (dueño) o estado (encargado/admin) |
| `DELETE` | `/api/reservas/{id}` | Borrar |

```json
// POST /api/reservas
{
  "cantidad": 2,
  "fechaReserva": "2026-12-25",
  "horarioLlegada": "19:30",
  "idUsuario": 1,
  "idRestaurant": 1
}
```

Reglas: la fecha va como `yyyy-MM-dd`, no puede ser anterior a hoy ni estar a
más de 3 meses, y el cupo se descuenta **por restaurante y por día**.
Estados: `PENDIENTE`, `ACEPTADA`, `CANCELADA`.

### Reseñas

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/resenas` | Crear reseña (el autor sale del token) |
| `GET` | `/api/resenas?id={restaurantId}` | Reseñas de un restaurante |
| `GET` | `/api/resenas/usuario/{id}` | Reseñas de un usuario |
| `GET` | `/api/resenas/{id}` | Detalle |
| `PUT` | `/api/resenas/{id}` | Editar (solo el autor) |
| `DELETE` | `/api/resenas/{id}` | Borrar (autor o admin) |

Calificación de `1` a `5`, una reseña por usuario y restaurante.

### Clientes

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/clientes` | Listar clientes (solo admin) |
| `GET` | `/api/clientes/{id}` | Ver cliente (él mismo o admin) |
| `PATCH` | `/api/clientes/{id}` | Editar datos |
| `DELETE` | `/api/clientes/{id}` | Borrar (409 si tiene reservas o reseñas) |

### Admin

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/admin/requests` | Encargados pendientes de aprobar |
| `PUT` | `/api/admin/approved/{usuarioId}` | Aprobar el encargado |

---

## 🧪 Tests

```bash
./mvnw test
```

Levantan la app contra una base H2 en memoria y cubren el flujo completo: registro y
login, aprobación de restaurantes, permisos por rol, reservas (incluidos los
casos inválidos), reseñas, clientes y cartas.

---

## 📄 Licencia

Proyecto educativo, de libre distribución para fines académicos.