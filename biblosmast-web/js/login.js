import { login, getSesion } from "./auth.js";
import { textoError } from "./api.js";
import { mostrarMensaje } from "./dom.js";

const formulario = document.getElementById("frmLogin");
const boton = document.getElementById("btnIngresar");
const mensaje = document.getElementById("mensaje");

// Si ya hay sesion, ir directo al menu
if (getSesion()) window.location.replace("menu.html");

formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const username = formulario.username.value.trim();
    const password = formulario.password.value;

    if (!username || !password) {
        mostrarMensaje(mensaje, "Escribe tu usuario y contraseña");
        return;
    }

    boton.disabled = true;
    mostrarMensaje(mensaje, "");
    try {
        await login(username, password);
        window.location.replace("menu.html");
    } catch (error) {
        mostrarMensaje(mensaje, textoError(error));
        formulario.password.value = "";
        formulario.password.focus();
    } finally {
        boton.disabled = false;
    }
});
