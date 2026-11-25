import { api, textoError } from "./api.js";
import { mostrarMensaje } from "./dom.js";
import { iniciarTema } from "./tema.js";

iniciarTema();
const $ = (id) => document.getElementById(id);
const token = new URLSearchParams(window.location.search).get("token");

if (!token) {
    mostrarMensaje($("mensaje"), "Este enlace no es válido. Pide uno nuevo desde «¿Olvidaste tu contraseña?».");
    $("btnRestablecer").disabled = true;
}

$("frmRestablecer").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const nueva = $("claveNueva").value;
    if (nueva.length < 8) return mostrarMensaje($("mensaje"), "La nueva contraseña debe tener al menos 8 caracteres");
    if (nueva !== $("claveConfirmar").value) return mostrarMensaje($("mensaje"), "Las contraseñas no coinciden");

    $("btnRestablecer").disabled = true;
    mostrarMensaje($("mensaje"), "");
    try {
        const respuesta = await api.post("/auth/restablecer", { token, claveNueva: nueva });
        $("frmRestablecer").reset();
        mostrarMensaje($("mensaje"), respuesta.mensaje, "ok");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
        $("btnRestablecer").disabled = false;
    }
});
