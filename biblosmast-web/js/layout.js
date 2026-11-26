// Estructura común de las páginas internas: menú lateral, barra superior, tema y tiempo de sesión.
// Se monta sola desde requireSesion(): las páginas no repiten este HTML.
import { icono } from "./iconos.js";
import { borrarSesion } from "./sesion.js";
import { mejorarTablas } from "./tablas.js";
import { alternarTema, iniciarTema, temaActual } from "./tema.js";

const PERSONAL = ["ADMIN", "BIBLIOTECARIO"];

const MENU = [
    { id: "inicio", href: "menu.html", texto: "Inicio", roles: null },
    { id: "libros", href: "libros.html", texto: "Libros", roles: null },
    { id: "ejemplares", href: "ejemplares.html", texto: "Ejemplares", roles: PERSONAL },
    { id: "prestamos", href: "prestamos.html", texto: "Préstamos", roles: PERSONAL },
    { id: "reservas", href: "reservas.html", texto: "Reservas", roles: PERSONAL },
    { id: "multas", href: "multas.html", texto: "Multas", roles: PERSONAL },
    { id: "catalogos", href: "catalogos.html", texto: "Autores y catálogos", roles: PERSONAL },
    { id: "usuarios", href: "usuarios.html", texto: "Usuarios", roles: ["ADMIN"] },
    { id: "auditoria", href: "auditoria.html", texto: "Auditoría", roles: ["ADMIN"] },
    { id: "mi-cuenta", href: "mi-cuenta.html", texto: "Mi cuenta", roles: ["USUARIO"] },
    { id: "perfil", href: "perfil.html", texto: "Mi perfil", roles: null },
];

export function iniciales(sesion) {
    return ((sesion?.nombres?.[0] ?? "") + (sesion?.apellidos?.[0] ?? "")).toUpperCase() || "?";
}

/** Minutos que le quedan al token (redondeado hacia arriba; 0 si ya venció). */
export function minutosRestantes(expiraEn, ahoraMs = Date.now()) {
    return Math.max(0, Math.ceil((expiraEn * 1000 - ahoraMs) / 60000));
}

function elemento(etiqueta, atributos = {}, texto = null) {
    const el = document.createElement(etiqueta);
    for (const [k, v] of Object.entries(atributos)) el.setAttribute(k, v);
    if (texto !== null) el.textContent = texto;
    return el;
}

export function montarLayout(sesion) {
    iniciarTema();
    const principal = document.querySelector("main.contenedor");
    if (!principal || document.querySelector(".app")) return;
    const pagina = document.body.dataset.pagina ?? "";

    // ----- menú lateral -----
    const lateral = elemento("aside", { class: "lateral", id: "lateral", "aria-label": "Menú principal" });
    const marca = elemento("a", { class: "marca", href: "menu.html" });
    marca.insertAdjacentHTML("beforeend", icono("logo"));
    marca.append(elemento("span", {}, "Biblosmast"));
    const nav = elemento("nav", { class: "menu-lateral" });
    for (const item of MENU) {
        if (item.roles && !item.roles.includes(sesion.rol)) continue;
        const enlace = elemento("a", { href: item.href });
        if (item.id === pagina) {
            enlace.classList.add("activo");
            enlace.setAttribute("aria-current", "page");
        }
        enlace.insertAdjacentHTML("beforeend", icono(item.id));
        enlace.append(elemento("span", {}, item.texto));
        nav.append(enlace);
    }
    lateral.append(marca, nav, elemento("div", { class: "lateral-pie" }, "Sistema de gestión de biblioteca"));

    // ----- barra superior -----
    const superior = elemento("header", { class: "superior" });
    const izquierda = elemento("div", { class: "izquierda" });
    const botonMenu = elemento("button", { class: "btn secundario icono boton-menu", type: "button", "aria-label": "Abrir menú" });
    botonMenu.insertAdjacentHTML("beforeend", icono("menu"));
    izquierda.append(botonMenu, elemento("span", { class: "titulo-pagina" }, principal.querySelector("h1")?.textContent ?? ""));

    const usuario = elemento("div", { class: "usuario" });
    const tiempo = elemento("span", { class: "sesion-tiempo", id: "sesionRestante", title: "Tiempo de vigencia del token JWT" });
    const botonTema = elemento("button", { class: "btn secundario icono", type: "button", id: "btnTema" });
    const pintarTema = () => {
        const oscuro = temaActual() === "oscuro";
        botonTema.replaceChildren();
        botonTema.insertAdjacentHTML("beforeend", icono(oscuro ? "sol" : "luna"));
        botonTema.setAttribute("aria-label", oscuro ? "Cambiar a tema claro" : "Cambiar a tema oscuro");
        botonTema.title = botonTema.getAttribute("aria-label");
    };
    pintarTema();
    botonTema.addEventListener("click", () => {
        alternarTema();
        pintarTema();
    });
    const avatar = elemento("a", { class: "avatar", href: "perfil.html", title: "Mi perfil", "aria-label": "Mi perfil" }, iniciales(sesion));
    const datos = elemento("span", { class: "datos-usuario", id: "usuarioActual" }, `${sesion.nombres} ${sesion.apellidos} · ${sesion.rol}`);
    const salir = elemento("button", { class: "btn secundario chico", type: "button", id: "btnSalir" }, "Cerrar sesión");
    usuario.append(tiempo, botonTema, avatar, datos, salir);
    superior.append(izquierda, usuario);

    // ----- ensamblado -----
    const contenido = elemento("div", { class: "contenido" });
    const fondo = elemento("div", { class: "fondo-menu" });
    const app = elemento("div", { class: "app" });
    document.body.prepend(app);
    contenido.append(superior, principal);
    app.append(lateral, contenido, fondo);

    const alternarMenu = (abierto) => app.classList.toggle("menu-abierto", abierto);
    botonMenu.addEventListener("click", () => alternarMenu(!app.classList.contains("menu-abierto")));
    fondo.addEventListener("click", () => alternarMenu(false));
    nav.addEventListener("click", () => alternarMenu(false));

    // ----- tiempo de sesión: avisa y, al vencer el token, vuelve al login -----
    function revisarSesion() {
        const minutos = minutosRestantes(sesion.expiraEn);
        if (Date.now() >= sesion.expiraEn * 1000) {
            borrarSesion();
            window.location.replace("index.html?motivo=expirada");
            return;
        }
        tiempo.textContent = `Sesión: ${minutos} min`;
        tiempo.classList.toggle("poco", minutos <= 5);
    }
    revisarSesion();
    setInterval(revisarSesion, 30000);

    mejorarTablas();
}
