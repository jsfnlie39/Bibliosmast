import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { crearDom, esperar, simularUbicacion } from "./ayuda-dom.js";

crearDom(readFileSync(new URL("../registro.html", import.meta.url), "utf-8"));
const redirecciones = [];
const { real } = simularUbicacion({ replace: (u) => redirecciones.push(u) });

let ultima = null;
function simularFetch(status, cuerpo) {
    globalThis.fetch = async (url, op) => { ultima = { url, op }; return { ok: status < 400, status, json: async () => cuerpo }; };
}
await import("../js/registro.js");

const llenar = (extra = {}) => {
    const v = { documento: "123", nombres: "Ana", apellidos: "Ruiz", email: "ana@x.com", telefono: "", username: "ana.ruiz", password: "Clave1234", confirmar: "Clave1234", ...extra };
    for (const [id, valor] of Object.entries(v)) document.getElementById(id).value = valor;
};
const enviar = async () => { document.getElementById("frmRegistro").dispatchEvent(new real.Event("submit", { cancelable: true })); await esperar(); await esperar(); };

test("las contraseñas distintas no se envían", async () => {
    simularFetch(201, {});
    llenar({ confirmar: "otra" });
    await enviar();
    assert.equal(ultima, null);
    assert.match(document.getElementById("mensaje").textContent, /no coinciden/);
});

test("registro correcto: envía los datos, guarda la sesión y entra al menú", async () => {
    simularFetch(201, { idUsuario: 9, username: "ana.ruiz", rol: "USUARIO", token: "t", expiraEn: 9999999999 });
    llenar();
    await enviar();
    assert.match(ultima.url, /\/auth\/registro$/);
    assert.deepEqual(JSON.parse(ultima.op.body), { documento: "123", nombres: "Ana", apellidos: "Ruiz", email: "ana@x.com", telefono: null, username: "ana.ruiz", password: "Clave1234" });
    assert.equal(JSON.parse(sessionStorage.getItem("biblosmast.sesion")).token, "t");
    assert.deepEqual(redirecciones, ["menu.html"]);
});

test("si el servidor rechaza (correo repetido) muestra el mensaje y no inicia sesión", async () => {
    sessionStorage.clear(); redirecciones.length = 0;
    simularFetch(409, { status: 409, mensaje: "Ya existe un usuario con ese correo" });
    llenar();
    await enviar();
    assert.match(document.getElementById("mensaje").textContent, /ese correo/);
    assert.equal(sessionStorage.getItem("biblosmast.sesion"), null);
    assert.deepEqual(redirecciones, []);
    assert.equal(document.getElementById("btnRegistro").disabled, false);
});
