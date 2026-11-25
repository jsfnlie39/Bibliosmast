import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { crearDom, esperar } from "./ayuda-dom.js";

crearDom();
const { celdaCsv, comparar, mejorarTabla, valorOrden } = await import("../js/tablas.js");

function crearTabla(filas, cabeceras = ["Título", "Año", "Fecha", ""]) {
    document.body.innerHTML = `<div class="tabla-wrap"><table><thead><tr>${cabeceras.map((c) => `<th>${c}</th>`).join("")}</tr></thead><tbody></tbody></table></div>`;
    const cuerpo = document.querySelector("tbody");
    const llenar = (lista) => {
        cuerpo.replaceChildren();
        for (const f of lista) {
            const tr = document.createElement("tr");
            tr.innerHTML = f.map((c) => `<td>${c}</td>`).join("");
            cuerpo.append(tr);
        }
    };
    llenar(filas);
    return { tabla: document.querySelector("table"), cuerpo, llenar };
}
const visibles = (cuerpo) => [...cuerpo.rows].filter((f) => !f.hidden).map((f) => f.cells[0].textContent);
const pie = () => document.querySelector(".tabla-pie");

beforeEach(() => { document.body.innerHTML = ""; });

test("valorOrden: fechas, números, «X de Y» y texto", () => {
    assert.equal(valorOrden("22/10/2026"), 20261022);
    assert.equal(valorOrden("22/10/2026, 10:00 a. m."), 20261022);
    assert.equal(valorOrden("1967"), 1967);
    assert.equal(valorOrden("3,5"), 3.5);
    assert.equal(valorOrden("1 de 2"), 1);
    assert.equal(valorOrden("  Álgebra "), "álgebra");
    assert.ok(comparar(valorOrden("9"), valorOrden("10")) < 0, "9 va antes que 10 (orden numérico)");
    assert.ok(comparar("ñandú", "zorro") < 0, "orden alfabético en español");
});

test("celdaCsv: comillas, saltos de línea y protección contra fórmulas", () => {
    assert.equal(celdaCsv('Dijo "hola"'), '"Dijo ""hola"""');
    assert.equal(celdaCsv("a\n  b"), '"a b"');
    assert.equal(celdaCsv("=SUMA(A1)"), `"'=SUMA(A1)"`);
    assert.equal(celdaCsv("-5"), `"'-5"`);
});

test("pagina de 10 en 10 y muestra el rango", async () => {
    const filas = Array.from({ length: 25 }, (_, i) => [`Libro ${String(i + 1).padStart(2, "0")}`, "2000", "01/01/2026", ""]);
    const { tabla, cuerpo } = crearTabla(filas);
    mejorarTabla(tabla);
    assert.equal(visibles(cuerpo).length, 10);
    assert.match(pie().textContent, /Mostrando 1–10 de 25/);
    pie().querySelectorAll("button")[1].click(); // siguiente
    assert.equal(visibles(cuerpo)[0], "Libro 11");
    pie().querySelectorAll("button")[1].click();
    assert.equal(visibles(cuerpo).length, 5);
    assert.match(pie().textContent, /Mostrando 21–25 de 25/);
    assert.equal(pie().querySelectorAll("button")[1].disabled, true, "en la última página no hay siguiente");
});

test("ordena al hacer clic: ascendente, descendente y por fecha o número", () => {
    const { tabla, cuerpo } = crearTabla([["Beta", "10", "05/03/2026", ""], ["Alfa", "9", "20/01/2026", ""], ["Gamma", "100", "01/12/2025", ""]]);
    mejorarTabla(tabla);
    const [thTitulo, thAnio, thFecha, thAcciones] = tabla.tHead.rows[0].cells;
    assert.equal(thAcciones.classList.contains("ordenable"), false, "la columna sin texto no se ordena");
    thTitulo.click();
    assert.deepEqual([...cuerpo.rows].map((f) => f.cells[0].textContent), ["Alfa", "Beta", "Gamma"]);
    assert.equal(thTitulo.dataset.orden, "asc");
    thTitulo.click();
    assert.deepEqual([...cuerpo.rows].map((f) => f.cells[0].textContent), ["Gamma", "Beta", "Alfa"]);
    assert.equal(thTitulo.dataset.orden, "desc");
    thAnio.click();
    assert.deepEqual([...cuerpo.rows].map((f) => f.cells[1].textContent), ["9", "10", "100"], "orden numérico, no de texto");
    assert.equal(thTitulo.dataset.orden, undefined);
    thFecha.click();
    assert.deepEqual([...cuerpo.rows].map((f) => f.cells[0].textContent), ["Gamma", "Alfa", "Beta"], "orden por fecha");
});

test("si la página vuelve a dibujar las filas, se reordena y pagina sola", async () => {
    const { tabla, cuerpo, llenar } = crearTabla([["B", "1", "", ""], ["A", "2", "", ""]]);
    mejorarTabla(tabla);
    tabla.tHead.rows[0].cells[0].click();
    llenar([["Z", "1", "", ""], ["M", "2", "", ""], ["C", "3", "", ""]]);
    await esperar();
    assert.deepEqual([...cuerpo.rows].map((f) => f.cells[0].textContent), ["C", "M", "Z"]);
    assert.match(pie().textContent, /Mostrando 1–3 de 3/);
});

test("la fila «No hay registros» no cuenta y el pie se oculta", async () => {
    const { tabla, cuerpo, llenar } = crearTabla([]);
    mejorarTabla(tabla);
    cuerpo.innerHTML = '<tr><td colspan="4" class="vacio">No hay registros</td></tr>';
    await esperar();
    assert.equal(pie().hidden, true);
    llenar([["Uno", "1", "", ""]]);
    await esperar();
    assert.equal(pie().hidden, false);
    assert.match(pie().textContent, /de 1/);
});

test("encabezados dibujados después (catálogos): se vuelven ordenables y cambiar de columnas reinicia el orden", async () => {
    document.body.innerHTML = '<div class="tabla-wrap"><table><thead><tr id="enc"></tr></thead><tbody></tbody></table></div>';
    const tabla = document.querySelector("table");
    const info = mejorarTabla(tabla);
    const poner = (cols, filas) => {
        document.getElementById("enc").innerHTML = cols.map((c) => `<th>${c}</th>`).join("");
        document.querySelector("tbody").innerHTML = filas.map((f) => `<tr>${f.map((c) => `<td>${c}</td>`).join("")}</tr>`).join("");
    };
    poner(["Nombre", "País"], [["Zeta", "X"], ["Alfa", "Y"]]);
    await esperar();
    const th = document.querySelector("th");
    assert.equal(th.classList.contains("ordenable"), true);
    th.click();
    assert.deepEqual([...document.querySelectorAll("tbody tr")].map((f) => f.cells[0].textContent), ["Alfa", "Zeta"]);
    poner(["Nombres", "Apellidos", "Nacionalidad"], [["B", "b", "x"], ["A", "a", "y"]]);
    await esperar();
    assert.equal(info.estado.columna, null, "otras columnas: el orden anterior ya no aplica");
});

test("CSV: encabezados con texto, todas las filas (no solo la página) y en el orden elegido", () => {
    const filas = Array.from({ length: 12 }, (_, i) => [`T${String(12 - i).padStart(2, "0")}`, String(i), "01/01/2026", "Editar"]);
    const { tabla } = crearTabla(filas, ["Título", "Año", "Fecha", ""]);
    const t = mejorarTabla(tabla);
    tabla.tHead.rows[0].cells[0].click();
    const lineas = t.generarCsv().split("\r\n");
    assert.equal(lineas[0], '"Título","Año","Fecha"');
    assert.equal(lineas.length, 13, "encabezado + 12 filas aunque la página muestre 10");
    assert.equal(lineas[1], '"T01","11","01/01/2026"');
});
