import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { crearDom } from "./ayuda-dom.js";

crearDom();
// jsdom no puede dibujar en un <canvas>; Chart.js avisa por consola y el panel sigue. Se silencia solo ese aviso.
const consolaError = console.error;
console.error = (...a) => { if (!String(a[0]).includes("Failed to create chart")) consolaError(...a); };

let peticiones = [];
function simularApi(datos) {
    peticiones = [];
    globalThis.fetch = async (url) => {
        const ruta = new URL(url).pathname.replace(/^\/api/, "");
        peticiones.push(ruta);
        const dato = datos[ruta];
        if (dato === undefined) return { ok: false, status: 403, json: async () => ({ mensaje: "No tienes permiso para realizar esta acción" }) };
        return { ok: true, status: 200, json: async () => dato };
    };
}
globalThis.sessionStorage = { getItem: () => null, setItem() {}, removeItem() {} };

const { cargarPanel } = await import("../js/panel.js");

const DATOS = {
    "/libros": [{ categoria: "Novela" }, { categoria: "Ciencia" }],
    "/ejemplares": [{ estado: "DISPONIBLE" }, { estado: "PRESTADO" }, { estado: "DISPONIBLE" }],
    "/prestamos": [{ estado: "ACTIVO", fechaPrestamo: "2026-11-06T10:00:00" }, { estado: "VENCIDO", fechaPrestamo: "2026-11-01T10:00:00" }, { estado: "FINALIZADO", fechaPrestamo: "2026-10-30T10:00:00" }],
    "/reservas": [{ estado: "PENDIENTE" }, { estado: "ATENDIDA" }],
    "/usuarios": [{}, {}, {}, {}],
};

beforeEach(() => {
    document.body.innerHTML = `<section id="kpis"></section>` + ["grafEjemplares", "grafPrestamos", "grafCategorias", "grafActividad"]
        .map((id) => `<article class="grafico-tarjeta"><div class="grafico-caja"><canvas id="${id}"></canvas></div></article>`).join("");
});

const kpis = () => [...document.querySelectorAll(".kpi")].map((k) => [k.querySelector(".kpi-etiqueta").textContent, k.querySelector(".kpi-valor").textContent]);
const tarjetaOculta = (id) => document.getElementById(id).closest(".grafico-tarjeta").hidden;

test("ADMIN: ve todos los indicadores y los cuatro gráficos", async () => {
    simularApi(DATOS);
    await cargarPanel({ rol: "ADMIN" });
    assert.deepEqual(kpis(), [
        ["Libros en el catálogo", "2"], ["Ejemplares disponibles", "2 de 3"], ["Préstamos activos", "1"],
        ["Préstamos vencidos", "1"], ["Reservas pendientes", "1"], ["Usuarios registrados", "4"],
    ]);
    assert.equal(document.querySelector(".kpi.alerta") !== null, true, "los vencidos se marcan en rojo cuando hay alguno");
    for (const id of ["grafEjemplares", "grafPrestamos", "grafCategorias", "grafActividad"]) assert.equal(tarjetaOculta(id), false, id);
});

test("BIBLIOTECARIO: no pide ni muestra usuarios", async () => {
    simularApi(DATOS);
    await cargarPanel({ rol: "BIBLIOTECARIO" });
    assert.equal(peticiones.includes("/usuarios"), false);
    assert.equal(kpis().some(([e]) => e === "Usuarios registrados"), false);
    assert.equal(kpis().length, 5);
});

test("USUARIO: solo libros; los gráficos que no puede ver se ocultan", async () => {
    simularApi(DATOS);
    await cargarPanel({ rol: "USUARIO" });
    assert.deepEqual(peticiones, ["/libros"]);
    assert.deepEqual(kpis(), [["Libros en el catálogo", "2"]]);
    assert.equal(tarjetaOculta("grafCategorias"), false);
    for (const id of ["grafEjemplares", "grafPrestamos", "grafActividad"]) assert.equal(tarjetaOculta(id), true, id);
});

test("un dato que la API rechaza (403) simplemente no se muestra y el resto sigue", async () => {
    const { "/ejemplares": _omitido, ...sinEjemplares } = DATOS;
    simularApi(sinEjemplares);
    await cargarPanel({ rol: "BIBLIOTECARIO" });
    assert.equal(kpis().some(([e]) => e === "Ejemplares disponibles"), false);
    assert.equal(kpis().some(([e]) => e === "Préstamos activos"), true);
    assert.equal(tarjetaOculta("grafEjemplares"), true);
});

test("sin vencidos, el indicador no se pinta como alerta", async () => {
    simularApi({ ...DATOS, "/prestamos": [{ estado: "ACTIVO", fechaPrestamo: "2026-11-06T10:00:00" }] });
    await cargarPanel({ rol: "ADMIN" });
    assert.equal(document.querySelector(".kpi.alerta"), null);
});
