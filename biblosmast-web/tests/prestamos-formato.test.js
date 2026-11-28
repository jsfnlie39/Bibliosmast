import { test } from "node:test";
import assert from "node:assert/strict";

const { puedeRenovar, textoRenovaciones, textoRenovado } = await import("../js/prestamos-formato.js");

test("puedeRenovar: solo los préstamos activos", () => {
  assert.equal(puedeRenovar({ estado: "ACTIVO" }), true);
  assert.equal(puedeRenovar({ estado: "VENCIDO" }), false);
  assert.equal(puedeRenovar({ estado: "FINALIZADO" }), false);
  assert.equal(puedeRenovar(null), false);
});

test("textoRenovaciones: cero, singular y plural", () => {
  assert.equal(textoRenovaciones(0), "Sin renovar");
  assert.equal(textoRenovaciones(undefined), "Sin renovar");
  assert.equal(textoRenovaciones(1), "1 vez");
  assert.equal(textoRenovaciones(2), "2 veces");
});

test("textoRenovado: incluye el número y la nueva fecha en dd/mm/aaaa", () => {
  assert.equal(
    textoRenovado({ idPrestamo: 7, fechaLimite: "2026-12-04" }),
    "Préstamo N° 7 renovado. Nueva fecha límite: 04/12/2026.",
  );
});
