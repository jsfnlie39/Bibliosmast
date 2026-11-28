import { test } from "node:test";
import assert from "node:assert/strict";

const { etiquetaMes, parametrosReporte, serieMeses, serieLibros, serieRetrasos } = await import("../js/reportes-formato.js");

test("etiquetaMes: mes abreviado y año; lo raro se devuelve igual", () => {
  assert.equal(etiquetaMes("2026-11"), "nov 2026");
  assert.equal(etiquetaMes("2026-01"), "ene 2026");
  assert.equal(etiquetaMes("2026-13"), "13 2026");
  assert.equal(etiquetaMes("raro"), "raro");
  assert.equal(etiquetaMes(undefined), "");
});

test("parametrosReporte: solo lo que tiene valor", () => {
  assert.equal(parametrosReporte({}), "");
  assert.equal(parametrosReporte({ desde: "", hasta: "", limite: "" }), "");
  assert.equal(parametrosReporte({ desde: "2026-01-01", hasta: "2026-11-24", limite: 10 }),
    "?desde=2026-01-01&hasta=2026-11-24&limite=10");
});

test("series: etiquetas y valores en el mismo orden, títulos largos acortados", () => {
  assert.deepEqual(serieMeses([{ mes: "2026-10", prestamos: 3 }, { mes: "2026-11", prestamos: 0 }]),
    { etiquetas: ["oct 2026", "nov 2026"], valores: [3, 0] });
  const libros = serieLibros([{ titulo: "Cosmos", prestamos: 5 }, { titulo: "x".repeat(40), prestamos: 2 }], 10);
  assert.deepEqual(libros.valores, [5, 2]);
  assert.equal(libros.etiquetas[0], "Cosmos");
  assert.equal(libros.etiquetas[1], `${"x".repeat(9)}…`);
  assert.deepEqual(serieRetrasos([{ usuario: "Ana Ruiz", multasRetraso: 2, ejemplaresVencidosAhora: 1 }]),
    { etiquetas: ["Ana Ruiz"], multas: [2], vencidos: [1] });
});
