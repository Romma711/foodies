// Leaves the database in a known demo state through the public API.
// Usage (backend running): node docs/db/seed-demo.mjs [http://localhost:8080]
//
// Re-runnable: every run converges to the same state.
// - Missing accounts are created; existing ones are reused, reactivated and their restaurant data restored.
// - Every reservation and review of the demo restaurants is deleted and created again,
//   so whatever was done during a rehearsal disappears.
// - Strikes cannot be reset through the API; they keep accumulating across runs.
// Data that does not belong to the demo restaurants is never touched.
// Every demo account uses DEMO_PASSWORD.

const API = (process.argv[2] ?? 'http://localhost:8080') + '/api';
const DEMO_PASSWORD = 'demo1234';
const ADMIN = { email: 'admin@foodies.com', password: 'admin123' };
const DOMINIO = 'demo.foodies.com';

// Fictional restaurants on real Mar del Plata streets; coordinates are approximate.
const RESTAURANTES = [
  { nombre: 'La Brasa de Güemes', especialidad: 'PARRILLA', direccion: 'Güemes 2850, Mar del Plata', telefono: '223 451-2850', cupo: 60, latitud: -38.0141, longitud: -57.5434 },
  { nombre: 'Don Ramón Parrilla', especialidad: 'PARRILLA', direccion: 'Av. Constitución 5230, Mar del Plata', telefono: '223 479-5230', cupo: 80, latitud: -37.9843, longitud: -57.5698 },
  { nombre: 'Kaiyō Sushi Bar', especialidad: 'ASIATICA', direccion: 'Alem 3650, Mar del Plata', telefono: '223 486-3650', cupo: 40, latitud: -38.0262, longitud: -57.5338 },
  { nombre: 'Trattoria Nonna Rosa', especialidad: 'PASTAS', direccion: 'Olavarría 2710, Mar del Plata', telefono: '223 492-2710', cupo: 50, latitud: -38.0135, longitud: -57.5452 },
  { nombre: 'Cantina del Puerto', especialidad: 'PESCADOS', direccion: 'Av. Martínez de Hoz 258, Mar del Plata', telefono: '223 480-0258', cupo: 90, latitud: -38.0447, longitud: -57.5371 },
  { nombre: 'Café de la Rambla', especialidad: 'CAFE', direccion: 'Bv. Marítimo 2290, Mar del Plata', telefono: '223 494-2290', cupo: 30, latitud: -38.0005, longitud: -57.5432 },
  { nombre: 'El Rincón de Mirta', especialidad: 'MINUTAS', direccion: 'Av. Colón 3120, Mar del Plata', telefono: '223 473-3120', cupo: 35, latitud: -38.0063, longitud: -57.5562 },
];

// The last client is the one used live during the demo: no reviews and no reservations.
const CLIENTES = [
  ['Ana', 'Gómez'], ['Bruno', 'Díaz'], ['Carla', 'Ruiz'], ['Diego', 'Sosa'],
  ['Elena', 'Paz'], ['Facundo', 'Ríos'], ['Gala', 'Torres'], ['Hernán', 'Vega'],
  ['Lucía', 'Fernández'],
];

// [client index, restaurant index, rating, comment]
const RESENAS = [
  [0, 0, 5, 'El ojo de bife salió en su punto justo. Atención cálida y rápida.'],
  [1, 0, 4, 'Muy buena parrilla, las mollejas son imperdibles. Un poco ruidoso el salón.'],
  [2, 0, 5, 'Fuimos por un cumpleaños y nos atendieron de diez.'],
  [3, 1, 4, 'Porciones enormes y buen precio. Ideal para ir en grupo.'],
  [4, 1, 3, 'La carne bien, pero esperamos casi una hora un sábado.'],
  [0, 2, 5, 'El mejor sushi de la zona de Playa Grande, todo muy fresco.'],
  [5, 2, 4, 'Muy rico el ramen; el local es chico, conviene reservar.'],
  [1, 3, 5, 'Los sorrentinos de calabaza son como los de la abuela.'],
  [6, 3, 4, 'Pastas caseras y buena carta de vinos.'],
  [2, 4, 4, 'Rabas crocantes y la cazuela de mariscos muy completa.'],
  [3, 4, 3, 'Rico el pescado, pero el servicio fue lento.'],
  [4, 5, 5, 'Café de especialidad con vista al mar. Las medialunas, recién hechas.'],
  [5, 5, 4, 'Buen lugar para trabajar un rato a la mañana.'],
  [6, 6, 4, 'La milanesa a caballo es enorme y sale rapidísimo.'],
  [7, 6, 2, 'Pésima atención, el mozo nos trató de mala manera. No vuelvo nunca más, son unos inútiles.'],
];

// [client index, restaurant index, days from today, time, people, final state]
// ACEPTADA is set by the restaurant manager; CANCELADA by the client, as the ownership rules require.
const RESERVAS = [
  [0, 0, 1, '21:00', 4, 'PENDIENTE'],
  [1, 0, 2, '13:30', 2, 'ACEPTADA'],
  [2, 0, 3, '21:30', 6, 'CANCELADA'],
  [3, 1, 1, '20:30', 5, 'ACEPTADA'],
  [4, 1, 4, '13:00', 3, 'PENDIENTE'],
  [5, 2, 2, '21:00', 2, 'ACEPTADA'],
  [6, 2, 5, '20:00', 2, 'PENDIENTE'],
  [7, 3, 3, '13:30', 4, 'PENDIENTE'],
  [0, 3, 6, '21:00', 2, 'CANCELADA'],
  [1, 4, 2, '12:30', 6, 'ACEPTADA'],
  [2, 5, 1, '10:00', 2, 'PENDIENTE'],
  [3, 6, 4, '13:00', 3, 'ACEPTADA'],
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
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${text.slice(0, 200)}`);
  // 204 is how the backend answers an empty list.
  return res.status === 204 || !text ? null : safeJson(text);
}

function safeJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

const lista = (data) => (Array.isArray(data) ? data : []);
const login = async ({ email, password }) => (await call('POST', '/auth/login', { email, password })).Token;
const slug = (texto) => texto.normalize('NFD').replace(/[^\w]/g, '').toLowerCase();
const emailRestaurante = (r) => `${slug(r.nombre)}@${DOMINIO}`;
const emailCliente = ([nombre, apellido]) => `${slug(nombre)}.${slug(apellido)}@${DOMINIO}`;

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
}

/** Creates the account; an existing account is fine because the next steps restore its data. */
async function registrarSiFalta(email, registrar) {
  try {
    await registrar();
    console.log(`  creada: ${email}`);
  } catch (error) {
    console.log(`  ya existía: ${email}`);
  }
}

function fechaEnDias(dias) {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + dias);
  const dosDigitos = (n) => String(n).padStart(2, '0');
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`;
}

async function prepararRestaurantes(admin) {
  console.log('Restaurantes');
  for (const r of RESTAURANTES) {
    await registrarSiFalta(emailRestaurante(r), () => registrarRestaurante({
      email: emailRestaurante(r),
      password: DEMO_PASSWORD,
      nombreRestaurante: r.nombre,
      direccion: r.direccion,
      telefono: r.telefono,
      especialidadDeComida: r.especialidad,
      cupoMaximo: r.cupo,
    }));
  }

  const entidades = lista(await call('GET', '/admin/entidades/restaurantes', null, admin));
  const restaurantes = [];
  for (const r of RESTAURANTES) {
    const entidad = entidades.find((e) => e.email?.toLowerCase() === emailRestaurante(r));
    if (!entidad) throw new Error(`No se encontró el restaurante de ${emailRestaurante(r)}`);
    if (entidad.estado !== 'APROBADO') await call('PUT', `/admin/restaurantes/${entidad.id}/aprobar`, null, admin);
    if (entidad.activo === false) await call('PATCH', `/admin/restaurantes/${entidad.id}/activo`, { activo: true }, admin);
    await call('PUT', `/admin/restaurantes/${entidad.id}/ubicacion`, { latitud: r.latitud, longitud: r.longitud }, admin);

    // Undo any change made from the restaurant panel during a rehearsal. Auto-acceptance stays off
    // so the restaurant has to confirm reservations by hand during the demo.
    const token = await login({ email: emailRestaurante(r), password: DEMO_PASSWORD });
    await call('PATCH', '/restaurantes/me', {
      nombre: r.nombre,
      ubicacion: r.direccion,
      cupoMaximo: r.cupo,
      especialidad: r.especialidad,
      porcentajeAutoAceptacion: 0,
    }, token);
    restaurantes.push({ id: entidad.id, token });
  }
  return restaurantes;
}

async function prepararClientes(admin) {
  console.log('Clientes');
  for (const [i, cliente] of CLIENTES.entries()) {
    const [nombre, apellido] = cliente;
    const email = emailCliente(cliente);
    await registrarSiFalta(email, () => call('POST', '/auth/register/cliente', {
      nombre, apellido, email, password: DEMO_PASSWORD, telefono: `223 50${i}-10${i}0`,
    }));
  }

  const entidades = lista(await call('GET', '/admin/entidades/clientes', null, admin));
  const clientes = [];
  for (const cliente of CLIENTES) {
    const email = emailCliente(cliente);
    const entidad = entidades.find((e) => e.email?.toLowerCase() === email);
    if (!entidad) throw new Error(`No se encontró el cliente ${email}`);
    if (entidad.activo === false) await call('PATCH', `/admin/usuarios/${entidad.id}/activo`, { activo: true }, admin);
    clientes.push({ id: entidad.id, token: await login({ email, password: DEMO_PASSWORD }) });
  }
  return clientes;
}

/** Deletes every reservation and review of the demo restaurants, whoever made them. */
async function limpiar(admin, restaurantes) {
  console.log('Limpieza de reservas y reseñas anteriores');
  let reservas = 0;
  let resenas = 0;
  for (const { id } of restaurantes) {
    for (const reserva of lista(await call('GET', `/reservas/restaurante/${id}`, null, admin))) {
      await call('DELETE', `/reservas/${reserva.id}`, null, admin);
      reservas++;
    }
    for (const resena of lista(await call('GET', `/resenas?id=${id}`))) {
      await call('DELETE', `/resenas/${resena.id}`, null, admin);
      resenas++;
    }
  }
  console.log(`  borradas: ${reservas} reservas, ${resenas} reseñas`);
}

async function main() {
  const admin = await login(ADMIN);
  const restaurantes = await prepararRestaurantes(admin);
  const clientes = await prepararClientes(admin);
  await limpiar(admin, restaurantes);

  console.log('Reseñas');
  for (const [c, r, calificacion, comentario] of RESENAS) {
    await call('POST', '/resenas', { comentario, calificacion, restaurantId: restaurantes[r].id }, clientes[c].token);
  }

  console.log('Reservas');
  for (const [c, r, dias, horarioLlegada, cantidad, estado] of RESERVAS) {
    const cliente = clientes[c];
    const reserva = await call('POST', '/reservas', {
      cantidad,
      fechaReserva: fechaEnDias(dias),
      horarioLlegada,
      idUsuario: Number(cliente.id),
      idRestaurant: restaurantes[r].id,
    }, cliente.token);
    if (estado === 'ACEPTADA') {
      await call('PATCH', `/reservas/${reserva.id}/estado`, { estado: 'ACEPTADA' }, restaurantes[r].token);
    } else if (estado === 'CANCELADA') {
      await call('PUT', `/reservas/${reserva.id}`, { estadoReserva: 'CANCELADA' }, cliente.token);
    }
  }

  console.log(`Listo: ${RESTAURANTES.length} restaurantes, ${CLIENTES.length} clientes, `
    + `${RESENAS.length} reseñas y ${RESERVAS.length} reservas.`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
