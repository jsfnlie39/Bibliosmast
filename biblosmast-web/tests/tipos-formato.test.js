import { test } from "node:test";
import assert from "node:assert/strict";

const { resumenTipo, cuerpoTipo, usoDeTipo } = await import("../js/tipos-formato.js");

test("resumenTipo: plurales y sin renovaciones", () => {
  assert.equal(resumenTipo({ maxLibros: 3, diasPrestamo: 7, maxRenovaciones: 2 }), "3 libros · 7 días · 2 renovaciones");
  assert.equal(resumenTipo({ maxLibros: 1, diasPrestamo: 1, maxRenovaciones: 1 }), "1 libro · 1 día · 1 renovación");
  assert.equal(resumenTipo({ maxLibros: 2, diasPrestamo: 5, maxRenovaciones: 0 }), "2 libros · 5 días · sin renovaciones");
  assert.equal(resumenTipo(null), "—");
});

test("cuerpoTipo: números enteros, vacío = null y nombre sin espacios sobrantes", () => {
  assert.deepEqual(
    cuerpoTipo({ nombre: "  Docente ", maxLibros: "5", diasPrestamo: "15", maxRenovaciones: "0" }),
    { nombre: "Docente", maxLibros: 5, diasPrestamo: 15, maxRenovaciones: 0 },
  );
  assert.deepEqual(
    cuerpoTipo({ nombre: "X", maxLibros: "", diasPrestamo: " ", maxRenovaciones: undefined }),
    { nombre: "X", maxLibros: null, diasPrestamo: null, maxRenovaciones: null },
  );
});

test("usoDeTipo: solo se elimina un tipo sin usuarios y que no sea EXTERNO", () => {
  assert.deepEqual(usoDeTipo({ nombre: "DOCENTE", cantidadUsuarios: 0 }), { texto: "0 usuarios", eliminable: true });
  assert.deepEqual(usoDeTipo({ nombre: "DOCENTE", cantidadUsuarios: 1 }), { texto: "1 usuario", eliminable: false });
  assert.deepEqual(usoDeTipo({ nombre: "EXTERNO", cantidadUsuarios: 0 }), { texto: "0 usuarios", eliminable: false });
});
