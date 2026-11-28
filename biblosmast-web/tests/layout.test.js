import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { crearDom } from "./ayuda-dom.js";

crearDom();
const { iniciales, minutosRestantes, montarLayout } = await import("../js/layout.js");
const { alternarTema, iniciarTema, temaActual } = await import("../js/tema.js");

const sesion = (rol) => ({ idUsuario: 1, nombres: "Ana María", apellidos: "Ruiz", rol, token: "x", expiraEn: Math.floor(Date.now() / 1000) + 3600 });

function pagina(id) {
    document.body.replaceChildren();
    document.body.dataset.pagina = id;
    document.body.innerHTML = '<main class="contenedor"><h1>Libros</h1><div class="tabla-wrap"><table><thead><tr><th>A</th></tr></thead><tbody></tbody></table></div></main><dialog id="d"></dialog>';
}
const enlaces = () => [...document.querySelectorAll(".menu-lateral a")].map((a) => a.textContent);

beforeEach(() => { localStorage.clear(); delete document.documentElement.dataset.tema; });

test("iniciales y minutos restantes", () => {
    assert.equal(iniciales({ nombres: "ana", apellidos: "ruiz" }), "AR");
    assert.equal(iniciales({}), "?");
    assert.equal(minutosRestantes(1000 + 60 * 30, 1000 * 1000), 30);
    assert.equal(minutosRestantes(1000 + 61, 1000 * 1000), 2, "redondea hacia arriba");
    assert.equal(minutosRestantes(10, 1000 * 1000), 0, "vencido = 0");
});

test("monta el menú lateral y la barra superior alrededor del contenido existente", () => {
    pagina("libros");
    montarLayout(sesion("ADMIN"));
    const contenido = document.querySelector(".app > .contenido");
    assert.ok(contenido.querySelector("main.contenedor"), "el <main> original quedó dentro del layout");
    assert.ok(document.getElementById("d"), "los diálogos siguen en la página");
    assert.equal(document.getElementById("usuarioActual").textContent, "Ana María Ruiz · ADMIN");
    assert.ok(document.getElementById("btnSalir"), "las páginas siguen encontrando #btnSalir");
    assert.equal(document.querySelector(".avatar").textContent, "AR");
    assert.equal(document.querySelector(".titulo-pagina").textContent, "Libros");
    assert.equal(document.querySelector(".menu-lateral a.activo").textContent, "Libros");
    assert.equal(document.querySelector(".menu-lateral a.activo").getAttribute("aria-current"), "page");
    assert.match(document.getElementById("sesionRestante").textContent, /Sesión: 60 min/);
});

test("el menú depende del rol", () => {
    pagina("libros"); montarLayout(sesion("ADMIN"));
    assert.deepEqual(enlaces(), ["Inicio", "Libros", "Ejemplares", "Préstamos", "Reservas", "Multas", "Autores y catálogos", "Reportes", "Usuarios", "Tipos de usuario", "Auditoría", "Mi cuenta", "Mi perfil"]);
    pagina("libros"); document.querySelector(".app")?.remove(); montarLayout(sesion("BIBLIOTECARIO"));
    assert.deepEqual(enlaces(), ["Inicio", "Libros", "Ejemplares", "Préstamos", "Reservas", "Multas", "Autores y catálogos", "Reportes", "Mi cuenta", "Mi perfil"]);
    pagina("libros"); montarLayout(sesion("USUARIO"));
    assert.deepEqual(enlaces(), ["Inicio", "Libros", "Mi cuenta", "Mi perfil"]);
});

test("el avatar lleva a Mi perfil", () => {
    pagina("libros");
    montarLayout(sesion("USUARIO"));
    assert.equal(document.querySelector("a.avatar").getAttribute("href"), "perfil.html");
});

test("no se monta dos veces", () => {
    pagina("libros");
    montarLayout(sesion("ADMIN"));
    montarLayout(sesion("ADMIN"));
    assert.equal(document.querySelectorAll(".app").length, 1);
});

test("el botón de menú abre y cierra el lateral en pantallas pequeñas", () => {
    pagina("libros");
    montarLayout(sesion("ADMIN"));
    const app = document.querySelector(".app");
    document.querySelector(".boton-menu").click();
    assert.equal(app.classList.contains("menu-abierto"), true);
    document.querySelector(".fondo-menu").click();
    assert.equal(app.classList.contains("menu-abierto"), false);
});

test("las tablas de la página se mejoran al montar", () => {
    pagina("libros");
    montarLayout(sesion("ADMIN"));
    assert.ok(document.querySelector(".tabla-pie"), "se creó el pie con paginación y CSV");
    assert.equal(document.querySelector("th").classList.contains("ordenable"), true);
});

test("tema: sigue lo guardado, alterna, guarda y avisa", () => {
    localStorage.setItem("biblosmast.tema", "oscuro");
    assert.equal(iniciarTema(), "oscuro");
    assert.equal(document.documentElement.dataset.tema, "oscuro");
    let aviso = null;
    window.addEventListener("biblosmast:tema", (e) => { aviso = e.detail; });
    assert.equal(alternarTema(), "claro");
    assert.equal(localStorage.getItem("biblosmast.tema"), "claro");
    assert.equal(aviso, "claro");
    assert.equal(temaActual(), "claro");
});

test("el botón de tema cambia el tema de la página", () => {
    pagina("libros");
    montarLayout(sesion("ADMIN"));
    const antes = temaActual();
    document.getElementById("btnTema").click();
    assert.notEqual(temaActual(), antes);
});

test("la barra superior tiene la campana de avisos, antes del avatar", () => {
  pagina("libros"); montarLayout(sesion("BIBLIOTECARIO"));
  const usuario = document.querySelector(".superior .campana-caja");
  assert.ok(usuario, "falta la campana");
  assert.equal(usuario.nextElementSibling?.classList.contains("avatar"), true);
  assert.equal(document.querySelector(".campana-panel").hidden, true);
});
