import { api, textoError } from "./api.js";
import { mostrarMensaje } from "./dom.js";
import { iniciarTema } from "./tema.js";

iniciarTema();
const $ = (id) => document.getElementById(id);

$("frmRecuperar").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const correo = $("correo").value.trim();
    if (!correo) {
        mostrarMensaje($("mensaje"), "Escribe tu correo");
        return;
    }
    $("btnRecuperar").disabled = true;
    mostrarMensaje($("mensaje"), "");
    try {
        // La respuesta es siempre la misma, exista o no el correo (no se revela quien esta registrado)
        const respuesta = await api.post("/auth/recuperar", { correo });
        mostrarMensaje($("mensaje"), respuesta.mensaje, "ok");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
    } finally {
        $("btnRecuperar").disabled = false;
    }
});
