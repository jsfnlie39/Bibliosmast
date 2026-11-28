import { api, textoError } from "./api.js";
import { mostrarMensaje } from "./dom.js";
import { guardarSesion } from "./sesion.js";
import { iniciarTema } from "./tema.js";

iniciarTema();
const $ = (id) => document.getElementById(id);

$("frmRegistro").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    if ($("password").value !== $("confirmar").value) {
        mostrarMensaje($("mensaje"), "Las contraseñas no coinciden");
        return;
    }
    const telefono = $("telefono").value.trim();
    $("btnRegistro").disabled = true;
    mostrarMensaje($("mensaje"), "");
    try {
        // El servidor crea la persona y su cuenta (rol USUARIO) y responde con la sesion ya iniciada
        const sesion = await api.post("/auth/registro", {
            documento: $("documento").value.trim(),
            nombres: $("nombres").value.trim(),
            apellidos: $("apellidos").value.trim(),
            email: $("email").value.trim(),
            telefono: telefono === "" ? null : telefono,
            username: $("username").value.trim(),
            password: $("password").value,
        });
        guardarSesion(sesion);
        window.location.replace("menu.html");
    } catch (error) {
        mostrarMensaje($("mensaje"), textoError(error));
        $("btnRegistro").disabled = false;
    }
});
