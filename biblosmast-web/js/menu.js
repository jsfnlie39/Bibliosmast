import { requireSesion, logout } from "./auth.js";

function iniciar() {
    const sesion = requireSesion();
    if (!sesion) return;
    document.getElementById("usuarioActual").textContent =
        `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`;
    document.getElementById("btnSalir").addEventListener("click", logout);

    // Catalogos, ejemplares, prestamos y reservas: solo para el personal de la biblioteca
    if (!["ADMIN", "BIBLIOTECARIO"].includes(sesion.rol)) {
        for (const id of ["modCatalogos", "modEjemplares", "modPrestamos", "modReservas"]) {
            const modulo = document.getElementById(id);
            modulo.classList.add("deshabilitado");
            modulo.removeAttribute("href");
            modulo.querySelector("p").textContent = "Solo para el personal de la biblioteca.";
        }
    }

    // Usuarios: solo para el administrador
    if (sesion.rol !== "ADMIN") {
        const modulo = document.getElementById("modUsuarios");
        modulo.classList.add("deshabilitado");
        modulo.removeAttribute("href");
        modulo.querySelector("p").textContent = "Solo para el administrador.";
    }
}

iniciar();
