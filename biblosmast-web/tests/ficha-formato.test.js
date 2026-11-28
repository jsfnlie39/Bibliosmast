import { test } from "node:test";
import assert from "node:assert/strict";

const { hayFiltros, parametrosBusqueda, opcionesDeFiltro, textoMovimiento, textoCupo } =
  await import("../js/ficha-formato.js");

test("parametrosBusqueda: solo los filtros con valor, y soloDisponibles solo si está marcado", () => {
  assert.equal(parametrosBusqueda({}), "");
  assert.equal(parametrosBusqueda({ q: "  ", idAutor: "", soloDisponibles: false }), "");
  assert.equal(
    parametrosBusqueda({ q: "clean code", idCategoria: "2", anioDesde: "2000", soloDisponibles: true }),
    "?q=clean+code&idCategoria=2&anioDesde=2000&soloDisponibles=true",
  );
  assert.equal(parametrosBusqueda({ idioma: "Español" }), "?idioma=Espa%C3%B1ol");
});

test("hayFiltros: detecta cualquier filtro activo", () => {
  assert.equal(hayFiltros({ q: "", idAutor: "", soloDisponibles: false }), false);
  assert.equal(hayFiltros({ q: "a" }), true);
  assert.equal(hayFiltros({ anioHasta: "1999" }), true);
  assert.equal(hayFiltros({ soloDisponibles: true }), true);
});

test("opcionesDeFiltro: sin repetidos, ordenadas y autores emparejados con su id", () => {
  const libros = [
    { idsAutores: [2, 1], autores: ["Zeta", "Alfa"], idCategoria: 5, categoria: "Ciencia", idEditorial: 9, editorial: "Norma", idioma: "Español" },
    { idsAutores: [1], autores: ["Alfa"], idCategoria: 5, categoria: "Ciencia", idEditorial: 8, editorial: "Anaya", idioma: "Inglés" },
    { idsAutores: [], autores: [], idCategoria: 4, categoria: "Arte", idEditorial: 9, editorial: "Norma", idioma: " " },
  ];
  const o = opcionesDeFiltro(libros);
  assert.deepEqual(o.autores, [{ id: 1, nombre: "Alfa" }, { id: 2, nombre: "Zeta" }]);
  assert.deepEqual(o.categorias.map((c) => c.nombre), ["Arte", "Ciencia"]);
  assert.deepEqual(o.editoriales.map((e) => e.nombre), ["Anaya", "Norma"]);
  assert.deepEqual(o.idiomas.map((i) => i.id), ["Español", "Inglés"]);
});

test("textoMovimiento: cada situación de un ejemplar prestado", () => {
  assert.equal(textoMovimiento({ fechaDevolucion: null, vencido: false }), "Sin devolver");
  assert.equal(textoMovimiento({ fechaDevolucion: null, vencido: true }), "Sin devolver (vencido)");
  assert.equal(textoMovimiento({ fechaDevolucion: "2026-11-01T10:00:00", estadoDevolucion: "BUEN_ESTADO" }), "Devuelto en buen estado");
  assert.equal(textoMovimiento({ fechaDevolucion: "2026-11-01T10:00:00", estadoDevolucion: "DANADO" }), "Devuelto dañado");
  assert.equal(textoMovimiento({ fechaDevolucion: "2026-11-01T10:00:00", estadoDevolucion: "PERDIDO" }), "Perdido");
});

test("textoCupo: libros sin devolver sobre el máximo del tipo", () => {
  assert.equal(textoCupo({ ejemplaresSinDevolver: 2, maxLibros: 3 }), "2 de 3 libros");
});
