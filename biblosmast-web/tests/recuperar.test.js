import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { crearDom, esperar } from "./ayuda-dom.js";

crearDom(readFileSync(new URL("../recuperar.html", import.meta.url), "utf-8"));
let ultima = null;
function simularFetch(status, cuerpo) {
    globalThis.fetch = async (url, op) => { ultima = { url, op }; return { ok: status < 400, status, json: async () => cuerpo }; };
}
await import("../js/recuperar.js");
const enviar = async () => { document.getElementById("frmRecuperar").dispatchEvent(new globalThis.window.Event("submit", { cancelable: true })); await esperar(); await esperar(); };

test("sin correo no llama a la API", async () => {
    simularFetch(202, {});
    await enviar();
    assert.equal(ultima, null);
    assert.match(document.getElementById("mensaje").textContent, /Escribe tu correo/);
});

test("envía el correo y muestra el mensaje genérico del servidor", async () => {
    simularFetch(202, { mensaje: "Si el correo está registrado, te enviamos un enlace" });
    document.getElementById("correo").value = "ana@x.com";
    await enviar();
    assert.match(ultima.url, /\/auth\/recuperar$/);
    assert.deepEqual(JSON.parse(ultima.op.body), { correo: "ana@x.com" });
    assert.match(document.getElementById("mensaje").textContent, /te enviamos un enlace/);
    assert.equal(document.getElementById("mensaje").classList.contains("ok"), true);
});

test("el límite de solicitudes (429) se muestra como error", async () => {
    simularFetch(429, { status: 429, mensaje: "Demasiadas solicitudes. Intenta de nuevo más tarde" });
    await enviar();
    assert.match(document.getElementById("mensaje").textContent, /Demasiadas solicitudes/);
    assert.equal(document.getElementById("mensaje").classList.contains("error"), true);
});
