// Loads demo data through the public API so the admin dashboard has something to show.
// Usage (backend running): node docs/db/seed-demo.mjs [http://localhost:8080]
// Every demo account uses the password below. Running it twice skips the accounts that already exist.

const API = (process.argv[2] ?? 'http://localhost:8080') + '/api';
const DEMO_PASSWORD = 'demo1234';
const ADMIN = { email: 'admin@foodies.com', password: 'admin123' };

const RESTAURANTES = [
  // Coordinates are approximate points in Mar del Plata for the map.
  { nombre: 'La Parrilla de Tito', especialidad: 'PARRILLA', latitud: -38.0023, longitud: -57.5545 },
  { nombre: 'Ochiwan', especialidad: 'ASIATICA', latitud: -37.9962, longitud: -57.5498 },
  { nombre: 'Pasta Nostra', especialidad: 'PASTAS', latitud: -38.0107, longitud: -57.5419 },
  { nombre: 'El Muelle', especialidad: 'PESCADOS', latitud: -38.0446, longitud: -57.5326 },
  { nombre: 'Café Tostado', especialidad: 'CAFE', latitud: -38.0068, longitud: -57.5378 },
  { nombre: 'Minutas Don Pepe', especialidad: 'MINUTAS', latitud: -37.991, longitud: -57.56 },
];

const CLIENTES = [
  ['Ana', 'Gómez'], ['Bruno', 'Díaz'], ['Carla', 'Ruiz'], ['Diego', 'Sosa'],
  ['Elena', 'Paz'], ['Facundo', 'Ríos'], ['Gala', 'Torres'], ['Hernán', 'Vega'],
];

// [client index, restaurant index, rating, comment]
const RESENAS = [
  [0, 0, 5, 'El asado estaba en su punto, volvería mañana.'],
  [0, 1, 4, 'Muy buen ramen, el local es chico pero cálido.'],
  [0, 2, 5, 'Los sorrentinos caseros son de otro planeta.'],
  [0, 3, 3, 'Rico el pescado, pero tardaron bastante.'],
  [0, 4, 4, 'Buen café de especialidad y medialunas frescas.'],
  [1, 0, 5, 'La mejor entraña que probé en la ciudad.'],
  [1, 1, 5, 'Sushi fresquísimo y muy buena atención.'],
  [1, 2, 4, 'Porciones abundantes y buen precio.'],
  [1, 5, 2, 'La milanesa llegó fría y algo seca.'],
  [2, 0, 4, 'Muy buena carne, el vino de la casa también.'],
  [2, 1, 5, 'Excelente gyozas, todo impecable.'],
  [2, 3, 4, 'Las rabas estaban crocantes y bien servidas.'],
  [2, 5, 3, 'Correcto, nada que destacar demasiado.'],
  [3, 0, 5, 'Atención de diez y parrilla abundante.'],
  [3, 2, 3, 'La salsa estaba bien, la pasta algo pasada.'],
  [3, 4, 5, 'El flat white es el mejor de la zona.'],
  [4, 1, 4, 'Muy rico el pad thai, un poco picante.'],
  [4, 3, 2, 'El lugar es lindo pero la comida no estuvo bien.'],
  [5, 4, 4, 'Ideal para trabajar un rato con buen café.'],
  [5, 5, 3, 'Precio justo, ambiente familiar y simple.'],
  [6, 2, 5, 'Los ñoquis del 29 son imperdibles.'],
  [7, 5, 1, 'Pedimos dos veces y se olvidaron del pedido.'],
];

// Reservations per restaurant index; the last `canceladas` of each are cancelled afterwards.
const RESERVAS = [
  { restaurante: 0, cantidad: 9, canceladas: 2 },
  { restaurante: 1, cantidad: 7, canceladas: 1 },
  { restaurante: 2, cantidad: 5, canceladas: 0 },
  { restaurante: 3, cantidad: 3, canceladas: 1 },
  { restaurante: 4, cantidad: 6, canceladas: 3 },
  { restaurante: 5, cantidad: 2, canceladas: 0 },
];

async function call(method, path, body, token) {
  const res = await fetch(API + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: token } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const data = text ? safeJson(text) : null;
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${text.slice(0, 200)}`);
  return data;
}

function safeJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

const login = async ({ email, password }) => (await call('POST', '/auth/login', { email, password })).Token;
const slug = (texto) => texto.normalize('NFD').replace(/[^\w]/g, '').toLowerCase();

// Restaurant sign-up is multipart and needs 3 to 5 photos: three 1x1 PNGs in warm tones.
const FOTOS_DEMO = [
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGN40RUHAAQuAdF9dnbFAAAAAElFTkSuQmCC',
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4dHIaAAUCAlLeoKX0AAAAAElFTkSuQmCC',
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGPYkOAAAAMUAVEONchDAAAAAElFTkSuQmCC',
];

async function registrarRestaurante(body) {
  const form = new FormData();
  form.append('datos', new Blob([JSON.stringify(body)], { type: 'application/json' }));
  FOTOS_DEMO.forEach((foto, i) =>
    form.append('fotos', new Blob([Buffer.from(foto, 'base64')], { type: 'image/png' }), `foto-${i}.png`),
  );
  form.append('portada', '0');
  const res = await fetch(API + '/auth/register/restaurante', { method: 'POST', body: form });
  const text = await res.text();
  if (!res.ok) throw new Error(`POST /auth/register/restaurante → ${res.status} ${text.slice(0, 200)}`);
  return text ? safeJson(text) : null;
}

/** Registers the account unless it already exists; returns the response or null. */
async function registrarSiFalta(path, body) {
  try {
    return path === '/auth/register/restaurante' ? await registrarRestaurante(body) : await call('POST', path, body);
  } catch (error) {
    console.warn(`  ya existía o falló: ${body.email} (${error.message.split('→')[1]?.trim()})`);
    return null;
  }
}

function fechaEnDias(dias) {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + dias);
  return fecha.toISOString().slice(0, 10);
}

async function main() {
  const admin = await login(ADMIN);
  console.log('Restaurantes');
  for (const [i, r] of RESTAURANTES.entries()) {
    const creado = await registrarSiFalta('/auth/register/restaurante', {
      email: `${slug(r.nombre)}@demo.foodies.com`,
      password: DEMO_PASSWORD,
      nombreRestaurante: r.nombre,
      direccion: `Av. Siempre Viva ${100 + i * 10}`,
      telefono: `11400000${i}`,
      especialidadDeComida: r.especialidad,
      cupoMaximo: 50,
    });
    if (creado) await call('PUT', `/admin/approved/${creado.id}`, null, admin);
  }
  const publicados = await call('GET', '/restaurantes');
  const idRestaurante = RESTAURANTES.map((r) => {
    const encontrado = publicados.find((p) => p.nombre === r.nombre);
    if (!encontrado) throw new Error(`No se encontró el restaurante ${r.nombre}`);
    return encontrado.id;
  });

  console.log('Ubicaciones');
  for (const [i, r] of RESTAURANTES.entries()) {
    await call('PUT', `/admin/restaurantes/${idRestaurante[i]}/ubicacion`, { latitud: r.latitud, longitud: r.longitud }, admin);
  }

  console.log('Clientes');
  const clientes = [];
  for (const [i, [nombre, apellido]] of CLIENTES.entries()) {
    const email = `${slug(nombre)}.${slug(apellido)}@demo.foodies.com`;
    await registrarSiFalta('/auth/register/cliente', {
      nombre, apellido, email, password: DEMO_PASSWORD, telefono: `11500000${i}`,
    });
    const token = await login({ email, password: DEMO_PASSWORD });
    const { usuarioId } = JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString());
    clientes.push({ token, id: usuarioId });
  }

  console.log('Reseñas');
  for (const [c, r, calificacion, comentario] of RESENAS) {
    try {
      await call('POST', '/resenas', { comentario, calificacion, restaurantId: idRestaurante[r] }, clientes[c].token);
    } catch (error) {
      console.warn(`  omitida: ${error.message}`);
    }
  }

  console.log('Reservas');
  // The backend checks capacity per date across all restaurants, so each reservation gets its own day.
  let dia = 1;
  for (const { restaurante, cantidad, canceladas } of RESERVAS) {
    for (let n = 0; n < cantidad; n++) {
      const cliente = clientes[(restaurante + n) % clientes.length];
      const reserva = await call('POST', '/reservas', {
        cantidad: 2 + (n % 4),
        fechaReserva: fechaEnDias(dia++),
        horarioLlegada: n % 2 ? '21:00' : '13:30',
        idUsuario: Number(cliente.id),
        idRestaurant: idRestaurante[restaurante],
      }, cliente.token);
      if (n >= cantidad - canceladas) {
        await call('PUT', `/reservas/${reserva.id}`, { cantidad: reserva.cantidad, estadoReserva: 'CANCELADA' }, admin);
      }
    }
  }
  console.log('Listo.');
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
