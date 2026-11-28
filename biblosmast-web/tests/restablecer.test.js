import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { crearDom, esperar, simularUbicacion } from "./ayuda-dom.js";

crearDom(readFileSync(new URL("../restablecer.html", import.meta.url), "utf-8"));
const { real } = simularUbicacion({ search: "?token=abc123" });
let ultima = null;
function simularFetch(status, cuerpo) {
    globalThis.fetch = async (url, op) => { ultima = { url, op }; return { ok: status < 400, status, json: async () => cuerpo }; };
}
await import("../js/restablecer.js");
const enviar = async () => { document.getElementById("frmRestablecer").dispatchEvent(new real.Event("submit", { cancelable: true })); await esperar(); await esperar(); };

test("valida longitud y coincidencia antes de llamar a la API", async () => {
    simularFetch(200, {});
    document.getElementById("claveNueva").value = "corta";
    document.getElementById("claveConfirmar").value = "corta";
    await enviar();
    assert.match(document.getElementById("mensaje").textContent, /al menos 8/);
    document.getElementById("claveNueva").value = "Clave1234";
    document.getElementById("claveConfirmar").value = "Otra12345";
    await enviar();
    assert.match(document.getElementById("mensaje").textContent, /no coinciden/);
    assert.equal(ultima, null);
});

test("envía el código del enlace y la nueva contraseña", async () => {
    simularFetch(200, { mensaje: "Contraseña cambiada. Ya puedes iniciar sesión." });
    document.getElementById("claveNueva").value = "Clave1234";
    document.getElementById("claveConfirmar").value = "Clave1234";
    await enviar();
    assert.match(ultima.url, /\/auth\/restablecer$/);
    assert.deepEqual(JSON.parse(ultima.op.body), { token: "abc123", claveNueva: "Clave1234" });
    assert.match(document.getElementById("mensaje").textContent, /Ya puedes iniciar sesión/);
});

test("un enlace vencido muestra el error del servidor", async () => {
    simularFetch(400, { status: 400, mensaje: "El enlace no es válido o ya venció" });
    document.getElementById("claveNueva").value = "Clave1234";
    document.getElementById("claveConfirmar").value = "Clave1234";
    await enviar();
    assert.match(document.getElementById("mensaje").textContent, /ya venció/);
});
