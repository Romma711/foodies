// Leaves the database in a known demo state through the public API.
// Usage (backend running): node docs/db/seed-demo.mjs [http://localhost:8080]
//
// Re-runnable: every run converges to the same state.
// - Missing accounts are created; existing ones are reused, reactivated and their restaurant data restored.
// - Every reservation and review of the demo restaurants is deleted and created again,
//   so whatever was done during a rehearsal disappears.
// - Each restaurant's espacios (salones, patios, barras) are restored: the first one is the
//   "Salón principal" created at sign-up; missing ones are created and extra ones deactivated.
// - Each restaurant's opening hours are replaced by the demo week, and its shifts (turnos, RF24)
//   by the demo shifts: missing ones are created and extra ones removed. The café keeps per-day
//   capacity, as an example of a restaurant without shifts.
// - A deactivated demo restaurant or demo manager account is reactivated.
// - Strikes cannot be reset through the API; they keep accumulating across runs. A client with
//   3 strikes cannot book (D6): its reservations are skipped with a warning.
// - Reviews are kept between runs and only missing ones are created: since D12 a client can only
//   review after the restaurant marked an attended reservation, which the API cannot create in the
//   past. On a fresh database the demo reviews are skipped with a warning.
// Data that does not belong to the demo restaurants is never touched.
// Every demo account uses DEMO_PASSWORD.

import zlib from 'node:zlib';

const API = (process.argv[2] ?? 'http://localhost:8080') + '/api';
const DEMO_PASSWORD = 'demo1234';
const ADMIN = { email: 'admin@foodies.com', password: 'admin123' };
const DOMINIO = 'demo.foodies.com';

// Fictional restaurants on real Mar del Plata streets; coordinates are approximate.
// `espacios[0]` is the "Salón principal" created at sign-up with its capacity.
const salon = (capacidad) => ({ tipo: 'SALON', nombre: 'Salón principal', capacidad });
// Opening hours: the same franjas every day, so no seed reservation falls on a closed day.
const DIAS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const todosLosDias = (...franjas) =>
  DIAS.flatMap((dia) => franjas.map(([apertura, cierre]) => ({ dia, apertura, cierre })));
const ALMUERZO_Y_CENA = todosLosDias(['12:00', '16:00'], ['20:00', '00:00']);
// Shifts of 90 minutes every day; each one ends before its opening range closes (D11).
const turnosTodosLosDias = (...inicios) =>
  DIAS.flatMap((dia) => inicios.map((inicio) => ({ dia, inicio, duracionMinutos: 90 })));
const TURNOS_ALMUERZO_Y_CENA = turnosTodosLosDias('12:00', '13:30', '20:00', '21:30', '22:30');
const TURNOS_CENA = turnosTodosLosDias('20:00', '21:30', '22:30');
const RESTAURANTES = [
  { nombre: 'La Brasa de Güemes', horarios: ALMUERZO_Y_CENA, turnos: TURNOS_ALMUERZO_Y_CENA, especialidad: 'PARRILLA', direccion: 'Güemes 2850, Mar del Plata', telefono: '223 451-2850', latitud: -38.0141, longitud: -57.5434,
    espacios: [salon(40), { tipo: 'PATIO', nombre: 'Patio cervecero', capacidad: 20 }] },
  { nombre: 'Don Ramón Parrilla', horarios: ALMUERZO_Y_CENA, turnos: TURNOS_ALMUERZO_Y_CENA, especialidad: 'PARRILLA', direccion: 'Av. Constitución 5230, Mar del Plata', telefono: '223 479-5230', latitud: -37.9843, longitud: -57.5698,
    espacios: [salon(50), { tipo: 'SALON', nombre: 'Salón de eventos', capacidad: 30 }] },
  { nombre: 'Kaiyō Sushi Bar', horarios: todosLosDias(['19:30', '00:00']), turnos: TURNOS_CENA, especialidad: 'ASIATICA', direccion: 'Alem 3650, Mar del Plata', telefono: '223 486-3650', latitud: -38.0262, longitud: -57.5338,
    espacios: [salon(28), { tipo: 'BARRA', nombre: 'Barra de sushi', capacidad: 12 }] },
  { nombre: 'Trattoria Nonna Rosa', horarios: ALMUERZO_Y_CENA, turnos: TURNOS_ALMUERZO_Y_CENA, especialidad: 'PASTAS', direccion: 'Olavarría 2710, Mar del Plata', telefono: '223 492-2710', latitud: -38.0135, longitud: -57.5452,
    espacios: [salon(35), { tipo: 'PATIO', nombre: 'Patio interno', capacidad: 15 }] },
  { nombre: 'Cantina del Puerto', horarios: ALMUERZO_Y_CENA, turnos: TURNOS_ALMUERZO_Y_CENA, especialidad: 'PESCADOS', direccion: 'Av. Martínez de Hoz 258, Mar del Plata', telefono: '223 480-0258', latitud: -38.0447, longitud: -57.5371,
    espacios: [salon(60), { tipo: 'PATIO', nombre: 'Terraza al puerto', capacidad: 22 }, { tipo: 'BARRA', nombre: 'Barra', capacidad: 8 }] },
  { nombre: 'Café de la Rambla', horarios: todosLosDias(['08:00', '20:00']), turnos: [], especialidad: 'CAFE', direccion: 'Bv. Marítimo 2290, Mar del Plata', telefono: '223 494-2290', latitud: -38.0005, longitud: -57.5432,
    espacios: [salon(18), { tipo: 'PATIO', nombre: 'Vereda', capacidad: 12 }] },
  { nombre: 'El Rincón de Mirta', horarios: ALMUERZO_Y_CENA, turnos: TURNOS_ALMUERZO_Y_CENA, especialidad: 'MINUTAS', direccion: 'Av. Colón 3120, Mar del Plata', telefono: '223 473-3120', latitud: -38.0063, longitud: -57.5562,
    espacios: [salon(35)] },
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

// [client index, restaurant index, espacio index, days from today, time, people, final state]
// ACEPTADA is set by the restaurant manager; CANCELADA by the client, as the ownership rules require.
// With shifts the time is a shift start. A client never has two active reservations the same day
// at the same restaurant, nor less than 2 hours apart at different restaurants (D1).
const RESERVAS = [
  [0, 0, 0, 1, '21:30', 4, 'PENDIENTE'],
  [1, 0, 1, 2, '13:30', 2, 'ACEPTADA'],
  [2, 0, 0, 3, '21:30', 6, 'CANCELADA'],
  [3, 1, 0, 1, '20:00', 5, 'ACEPTADA'],
  [4, 1, 1, 4, '13:30', 3, 'PENDIENTE'],
  [5, 2, 1, 2, '21:30', 2, 'ACEPTADA'],
  [6, 2, 0, 5, '20:00', 2, 'PENDIENTE'],
  [7, 3, 0, 3, '13:30', 4, 'PENDIENTE'],
  [0, 3, 1, 6, '21:30', 2, 'CANCELADA'],
  [1, 4, 1, 3, '12:00', 6, 'ACEPTADA'],
  [2, 5, 1, 1, '10:00', 2, 'PENDIENTE'],
  [3, 6, 0, 4, '13:30', 3, 'ACEPTADA'],
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

/** Runs one optional step; a business-rule refusal is reported and the seed goes on. */
async function intentar(descripcion, paso) {
  try {
    return await paso();
  } catch (error) {
    console.warn(`  aviso: ${descripcion}: ${error.message}`);
    return null;
  }
}
const login = async ({ email, password }) => (await call('POST', '/auth/login', { email, password })).Token;
const slug = (texto) => texto.normalize('NFD').replace(/[^\w]/g, '').toLowerCase();
const emailRestaurante = (r) => `${slug(r.nombre)}@${DOMINIO}`;
const emailCliente = ([nombre, apellido]) => `${slug(nombre)}.${slug(apellido)}@${DOMINIO}`;

// Restaurant sign-up is multipart and needs 3 to 5 photos. The seed draws three 960x600 PNG
// covers per restaurant (a diagonal gradient with soft light spots) in its cuisine's palette,
// so the demo shows real covers instead of empty squares without shipping image files.
const PALETAS = {
  PARRILLA: ['#5a1f0c', '#e0703a'],
  ASIATICA: ['#1f3b4d', '#d9534f'],
  PASTAS: ['#7a4a14', '#f2c14e'],
  PESCADOS: ['#0f4c5c', '#5fa8d3'],
  CAFE: ['#3e2723', '#c08a5b'],
  MINUTAS: ['#5d4037', '#f4a259'],
};
const PALETA_DEFAULT = ['#3f3a33', '#f26b3a'];
const FOTOS_POR_RESTAURANTE = 3;

const rgb = (hex) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));

/** Small deterministic PRNG, so every run draws the same covers. */
function prng(semilla) {
  let s = semilla >>> 0;
  return () => {
    s = (s * 1664525 + 1013904223) >>> 0;
    return s / 2 ** 32;
  };
}

function chunkPng(tipo, datos) {
  const largo = Buffer.alloc(4);
  largo.writeUInt32BE(datos.length);
  const cuerpo = Buffer.concat([Buffer.from(tipo, 'ascii'), datos]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(zlib.crc32(cuerpo));
  return Buffer.concat([largo, cuerpo, crc]);
}

function portadaPng(especialidad, variante, ancho = 960, alto = 600) {
  const [oscuro, claro] = (PALETAS[especialidad] ?? PALETA_DEFAULT).map(rgb);
  const azar = prng(especialidad.length * 7919 + variante * 104729);
  const angulo = azar() * Math.PI;
  const [dx, dy] = [Math.cos(angulo), Math.sin(angulo)];
  const luces = Array.from({ length: 6 }, () => ({
    x: azar() * ancho,
    y: azar() * alto,
    r: 60 + azar() * 160,
    fuerza: 0.12 + azar() * 0.18,
  }));
  const fila = ancho * 3 + 1;
  const crudo = Buffer.alloc(fila * alto);
  for (let y = 0; y < alto; y++) {
    crudo[y * fila] = 0; // PNG filter type "none"
    for (let x = 0; x < ancho; x++) {
      const t = Math.min(1, Math.max(0, ((x / ancho - 0.5) * dx + (y / alto - 0.5) * dy) + 0.5));
      let luz = 0;
      for (const l of luces) {
        const d2 = ((x - l.x) ** 2 + (y - l.y) ** 2) / (l.r * l.r);
        luz += l.fuerza * Math.exp(-d2);
      }
      for (let c = 0; c < 3; c++) {
        const base = oscuro[c] + (claro[c] - oscuro[c]) * t;
        crudo[y * fila + 1 + x * 3 + c] = Math.round(Math.min(255, base + (255 - base) * luz));
      }
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(ancho, 0);
  ihdr.writeUInt32BE(alto, 4);
  ihdr.set([8, 2, 0, 0, 0], 8); // 8-bit RGB, no interlace
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunkPng('IHDR', ihdr),
    chunkPng('IDAT', zlib.deflateSync(crudo, { level: 9 })),
    chunkPng('IEND', Buffer.alloc(0)),
  ]);
}

async function registrarRestaurante(body) {
  const form = new FormData();
  form.append('datos', new Blob([JSON.stringify(body)], { type: 'application/json' }));
  for (let i = 0; i < FOTOS_POR_RESTAURANTE; i++) {
    const foto = portadaPng(body.especialidadDeComida, i);
    form.append('fotos', new Blob([foto], { type: 'image/png' }), `foto-${i}.png`);
  }
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
      cupoMaximo: r.espacios[0].capacidad,
    }));
  }

  const entidades = lista(await call('GET', '/admin/entidades/restaurantes', null, admin));
  const restaurantes = [];
  for (const r of RESTAURANTES) {
    const entidad = entidades.find((e) => e.email?.toLowerCase() === emailRestaurante(r));
    if (!entidad) throw new Error(`No se encontró el restaurante de ${emailRestaurante(r)}`);
    if (entidad.estado !== 'APROBADO') await call('PUT', `/admin/restaurantes/${entidad.id}/aprobar`, null, admin);
    if (entidad.activo === false) await call('PATCH', `/admin/restaurantes/${entidad.id}/activo`, { activo: true }, admin);
    // A deactivated manager cannot log in and hides the restaurant: always reactivate it (a no-op when active).
    await call('PATCH', `/admin/usuarios/${entidad.usuarioId}/activo`, { activo: true }, admin);
    await call('PUT', `/admin/restaurantes/${entidad.id}/ubicacion`, { latitud: r.latitud, longitud: r.longitud }, admin);

    // Undo any change made from the restaurant panel during a rehearsal. Auto-acceptance stays off
    // so the restaurant has to confirm reservations by hand during the demo.
    const token = await login({ email: emailRestaurante(r), password: DEMO_PASSWORD });
    await call('PATCH', '/restaurantes/me', {
      nombre: r.nombre,
      ubicacion: r.direccion,
      especialidad: r.especialidad,
      porcentajeAutoAceptacion: 0,
    }, token);
    await call('PUT', '/restaurantes/me/horarios', r.horarios, token);
    restaurantes.push({ id: entidad.id, token, espacios: r.espacios, turnos: r.turnos });
  }
  return restaurantes;
}

/**
 * Leaves each restaurant with exactly its demo espacios, active and with their capacity. Runs after
 * the cleanup, because an espacio with future reservations cannot shrink or be deactivated.
 * The first existing espacio is the "Salón principal" even if it was renamed during a rehearsal.
 */
async function prepararEspacios(restaurantes) {
  console.log('Espacios');
  for (const restaurante of restaurantes) {
    const existentes = lista(await call('GET', '/restaurantes/me/espacios', null, restaurante.token));
    const usados = new Set();
    restaurante.espacioIds = [];
    for (const [i, deseado] of restaurante.espacios.entries()) {
      const actual = i === 0
        ? existentes[0]
        : existentes.find((e) => !usados.has(e.id) && e.nombre.toLowerCase() === deseado.nombre.toLowerCase());
      let id;
      if (actual) {
        if (!actual.activo) await call('PATCH', `/restaurantes/me/espacios/${actual.id}/activo`, { activo: true }, restaurante.token);
        await call('PUT', `/restaurantes/me/espacios/${actual.id}`, deseado, restaurante.token);
        id = actual.id;
      } else {
        id = (await call('POST', '/restaurantes/me/espacios', deseado, restaurante.token)).id;
      }
      usados.add(id);
      restaurante.espacioIds.push(id);
    }
    for (const sobrante of existentes.filter((e) => !usados.has(e.id) && e.activo)) {
      await call('PATCH', `/restaurantes/me/espacios/${sobrante.id}/activo`, { activo: false }, restaurante.token);
    }
  }
}

/**
 * Leaves each restaurant with exactly its demo shifts. Runs after the cleanup, because a shift with
 * future reservations cannot be changed or removed.
 */
async function prepararTurnos(restaurantes) {
  console.log('Turnos');
  for (const restaurante of restaurantes) {
    const clave = (t) => `${t.dia} ${t.inicio} ${t.duracionMinutos}`;
    const deseados = new Set(restaurante.turnos.map(clave));
    const existentes = lista(await call('GET', '/restaurantes/me/turnos', null, restaurante.token));
    for (const sobrante of existentes.filter((t) => !deseados.has(clave(t)))) {
      await call('DELETE', `/restaurantes/me/turnos/${sobrante.id}`, null, restaurante.token);
    }
    const presentes = new Set(existentes.map(clave));
    for (const turno of restaurante.turnos.filter((t) => !presentes.has(clave(t)))) {
      await call('POST', '/restaurantes/me/turnos', turno, restaurante.token);
    }
  }
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

/**
 * Deletes every reservation of the demo restaurants, whoever made them. Reviews are kept: they can
 * only be created again after an attended reservation (D12).
 */
async function limpiar(admin, restaurantes) {
  console.log('Limpieza de reservas anteriores');
  let reservas = 0;
  for (const { id } of restaurantes) {
    for (const reserva of lista(await call('GET', `/reservas/restaurante/${id}`, null, admin))) {
      await call('DELETE', `/reservas/${reserva.id}`, null, admin);
      reservas++;
    }
  }
  console.log(`  borradas: ${reservas} reservas`);
}

async function main() {
  const admin = await login(ADMIN);
  const restaurantes = await prepararRestaurantes(admin);
  const clientes = await prepararClientes(admin);
  await limpiar(admin, restaurantes);
  await prepararEspacios(restaurantes);
  await prepararTurnos(restaurantes);

  console.log('Reseñas');
  let resenas = 0;
  for (const [c, r, calificacion, comentario] of RESENAS) {
    const existentes = lista(await call('GET', `/resenas/usuario/${clientes[c].id}`, null, admin));
    if (existentes.some((x) => x.restauranteId === restaurantes[r].id)) {
      resenas++;
      continue;
    }
    const creada = await intentar(`reseña de ${CLIENTES[c].join(' ')}`, () => call('POST', '/resenas',
      { comentario, calificacion, restaurantId: restaurantes[r].id }, clientes[c].token));
    if (creada) resenas++;
  }

  console.log('Reservas');
  let reservas = 0;
  for (const [c, r, e, dias, horarioLlegada, cantidad, estado] of RESERVAS) {
    const cliente = clientes[c];
    const reserva = await intentar(`reserva de ${CLIENTES[c].join(' ')}`, () => call('POST', '/reservas', {
      cantidad,
      fechaReserva: fechaEnDias(dias),
      horarioLlegada,
      idUsuario: Number(cliente.id),
      idRestaurant: restaurantes[r].id,
      idEspacio: restaurantes[r].espacioIds[e],
    }, cliente.token));
    if (!reserva) continue;
    reservas++;
    if (estado === 'ACEPTADA') {
      await call('PATCH', `/reservas/${reserva.id}/estado`, { estado: 'ACEPTADA' }, restaurantes[r].token);
    } else if (estado === 'CANCELADA') {
      await call('PUT', `/reservas/${reserva.id}`, { estadoReserva: 'CANCELADA' }, cliente.token);
    }
  }

  console.log(`Listo: ${RESTAURANTES.length} restaurantes, ${CLIENTES.length} clientes, `
    + `${resenas}/${RESENAS.length} reseñas y ${reservas}/${RESERVAS.length} reservas.`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
