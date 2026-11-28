import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const ej = JSON.parse(readFileSync(new URL("./ejemplo-jwt.json", import.meta.url)));

// --- entorno de navegador simulado ---
const almacen = new Map();
globalThis.sessionStorage = {
  getItem: (k) => (almacen.has(k) ? almacen.get(k) : null),
  setItem: (k, v) => almacen.set(k, String(v)),
  removeItem: (k) => almacen.delete(k),
};
const redirecciones = [];
globalThis.window = { location: { replace: (url) => redirecciones.push(url) } };
let ultimaPeticion = null;
function simularFetch(status, cuerpo) {
  globalThis.fetch = async (url, opciones) => {
    ultimaPeticion = { url, opciones };
    return { ok: status >= 200 && status < 300, status, json: async () => cuerpo };
  };
}

// requireSesion() monta el menú lateral; aquí no hay página, así que el documento simulado no tiene <main>
globalThis.document = { querySelector: () => null, documentElement: { dataset: {} }, body: { dataset: {} } };
globalThis.localStorage = { getItem: () => null, setItem: () => {} };

const { api, ApiError } = await import("../js/api.js");
const { login, requireSesion, logout } = await import("../js/auth.js");
const { decodificarJwt, tokenVencido, getSesion, esAdmin } = await import("../js/sesion.js");

const SESION = (extra = {}) => ({ idUsuario: 1, username: "admin", nombres: "A", apellidos: "S", rol: "ADMIN", token: ej.token, tipoToken: "Bearer", expiraEn: Math.floor(Date.now() / 1000) + 3600, ...extra });

beforeEach(() => { almacen.clear(); redirecciones.length = 0; ultimaPeticion = null; });

test("login correcto guarda la sesión con el token", async () => {
  simularFetch(200, SESION());
  await login("admin", "Admin123*");
  assert.equal(getSesion().token, ej.token);
  assert.deepEqual(JSON.parse(ultimaPeticion.opciones.body), { username: "admin", password: "Admin123*" });
  assert.equal(ultimaPeticion.opciones.headers.Authorization, undefined, "el login no lleva token");
});

test("con sesión, cada petición lleva Authorization: Bearer <token>", async () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION()));
  simularFetch(200, []);
  await api.get("/libros");
  assert.equal(ultimaPeticion.opciones.headers.Authorization, `Bearer ${ej.token}`);
  await api.post("/libros", { a: 1 });
  assert.equal(ultimaPeticion.opciones.headers.Authorization, `Bearer ${ej.token}`);
  assert.equal(ultimaPeticion.opciones.headers["Content-Type"], "application/json");
});

test("sin sesión no se envía Authorization", async () => {
  simularFetch(200, []);
  await api.get("/libros");
  assert.equal(ultimaPeticion.opciones.headers.Authorization, undefined);
});

test("401 fuera del login: borra la sesión, vuelve al login y lanza el error", async () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION()));
  simularFetch(401, { status: 401, mensaje: "Tu sesión expiró. Inicia sesión de nuevo" });
  await assert.rejects(() => api.get("/libros"), (e) => e instanceof ApiError && e.status === 401 && /expiró/.test(e.message));
  assert.equal(getSesion(), null);
  assert.deepEqual(redirecciones, ["index.html?motivo=expirada"]);
});

test("401 en el login (clave mala) no redirige y muestra el mensaje", async () => {
  simularFetch(401, { status: 401, mensaje: "Usuario o contraseña incorrectos" });
  await assert.rejects(() => login("admin", "mala"), (e) => e.status === 401 && e.message === "Usuario o contraseña incorrectos");
  assert.deepEqual(redirecciones, []);
  assert.equal(getSesion(), null);
});

test("403 (rol sin permiso) mantiene la sesión y no redirige", async () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION({ rol: "USUARIO" })));
  simularFetch(403, { status: 403, mensaje: "No tienes permiso para realizar esta acción" });
  await assert.rejects(() => api.get("/usuarios"), (e) => e.status === 403);
  assert.notEqual(getSesion(), null);
  assert.deepEqual(redirecciones, []);
});

test("requireSesion: sin sesión, con token vencido y con token vigente", () => {
  assert.equal(requireSesion(), null);
  assert.deepEqual(redirecciones, ["index.html"]);

  redirecciones.length = 0;
  almacen.set("biblosmast.sesion", JSON.stringify(SESION({ expiraEn: Math.floor(Date.now() / 1000) - 10 })));
  assert.equal(requireSesion(), null);
  assert.deepEqual(redirecciones, ["index.html?motivo=expirada"]);
  assert.equal(getSesion(), null);

  redirecciones.length = 0;
  almacen.set("biblosmast.sesion", JSON.stringify(SESION()));
  assert.equal(requireSesion().username, "admin");
  assert.deepEqual(redirecciones, []);
});

test("tokenVencido", () => {
  assert.equal(tokenVencido(null), true);
  assert.equal(tokenVencido({}), true);
  assert.equal(tokenVencido({ token: "x", expiraEn: 1 }), true);
  assert.equal(tokenVencido({ token: "x", expiraEn: Date.now() / 1000 + 60 }), false);
});

test("decodificarJwt lee encabezado y contenido de un token firmado por otra herramienta", () => {
  const { header, payload } = decodificarJwt(ej.token);
  assert.deepEqual(header, { alg: "HS256" });
  assert.deepEqual(payload, { sub: "admin", uid: 1, rol: "ADMIN", iat: ej.ahora, exp: ej.ahora + 3600 });
  assert.equal(decodificarJwt(ej.token2).payload.sub, "niño", "respeta UTF-8");
});

test("logout borra la sesión y vuelve al login", async () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION()));
  simularFetch(204, null);
  await logout();
  assert.equal(getSesion(), null);
  assert.deepEqual(redirecciones, ["index.html"]);
});

test("logout avisa al servidor con el token y limpia la sesión", async () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION()));
  simularFetch(204, null);
  await logout();
  assert.match(ultimaPeticion.url, /\/auth\/logout$/);
  assert.equal(ultimaPeticion.opciones.method, "POST");
  assert.equal(ultimaPeticion.opciones.headers.Authorization, `Bearer ${ej.token}`);
  assert.equal(getSesion(), null);
  assert.deepEqual(redirecciones, ["index.html"]);
});

test("logout cierra la sesión aunque el servidor no responda", async () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION()));
  globalThis.fetch = async () => { throw new TypeError("sin conexión"); };
  await logout();
  assert.equal(getSesion(), null);
  assert.deepEqual(redirecciones, ["index.html"]);
});

test("si el token ya estaba cerrado, el 401 del logout no se presenta como «sesión expirada»", async () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION()));
  simularFetch(401, { status: 401, mensaje: "Tu sesión fue cerrada. Inicia sesión de nuevo" });
  await logout();
  assert.deepEqual(redirecciones, ["index.html"]);
});

test("con clave temporal, requireSesion solo deja entrar a Mi perfil", () => {
  almacen.set("biblosmast.sesion", JSON.stringify(SESION({ debeCambiarClave: true })));
  document.body.dataset.pagina = "libros";
  assert.equal(requireSesion(), null);
  assert.deepEqual(redirecciones, ["perfil.html?cambiar=1"]);

  redirecciones.length = 0;
  document.body.dataset.pagina = "perfil";
  assert.equal(requireSesion().username, "admin");
  assert.deepEqual(redirecciones, []);
  delete document.body.dataset.pagina;
});

test("esAdmin solo es verdadero para el rol ADMIN", () => {
  assert.equal(esAdmin(), false, "sin sesión");
  almacen.set("biblosmast.sesion", JSON.stringify(SESION({ rol: "BIBLIOTECARIO" })));
  assert.equal(esAdmin(), false);
  almacen.set("biblosmast.sesion", JSON.stringify(SESION({ rol: "ADMIN" })));
  assert.equal(esAdmin(), true);
});
