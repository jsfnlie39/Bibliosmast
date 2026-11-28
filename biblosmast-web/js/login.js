import { login, getSesion } from "./auth.js";
import { textoError } from "./api.js";
import { borrarSesion, tokenVencido } from "./sesion.js";
import { mostrarMensaje } from "./dom.js";
import { iniciarTema } from "./tema.js";

iniciarTema();

const formulario = document.getElementById("frmLogin");
const boton = document.getElementById("btnIngresar");
const mensaje = document.getElementById("mensaje");

// Si ya hay una sesion con token vigente, ir directo al menu; si el token venció, se descarta
const sesionActual = getSesion();
if (sesionActual && !tokenVencido(sesionActual)) {
    window.location.replace("menu.html");
} else if (sesionActual) {
    borrarSesion();
}

// Llegamos aqui desde otra pagina porque el servidor rechazo el token (vencido o invalido)
if (new URLSearchParams(window.location.search).get("motivo") === "expirada") {
    mostrarMensaje(mensaje, "Tu sesión expiró. Inicia sesión de nuevo.");
}

formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const username = formulario.username.value.trim();
    const password = formulario.password.value;

    if (!username || !password) {
        mostrarMensaje(mensaje, "Escribe tu usuario o correo y tu contraseña");
        return;
    }

    boton.disabled = true;
    mostrarMensaje(mensaje, "");
    try {
        const sesion = await login(username, password);
        window.location.replace(sesion.debeCambiarClave ? "perfil.html?cambiar=1" : "menu.html");
    } catch (error) {
        mostrarMensaje(mensaje, textoError(error));
        formulario.password.value = "";
        formulario.password.focus();
    } finally {
        boton.disabled = false;
    }
});
