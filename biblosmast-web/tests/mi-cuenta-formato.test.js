import { test } from "node:test";
import assert from "node:assert/strict";

const {
  formatearFecha, textoMotivoMulta, puedeCancelarReserva, textoSituacionReserva,
  librosReservables, textoMultasPendientes,
} = await import("../js/mi-cuenta-formato.js");

test("formatearFecha: dd/mm/aaaa y guion si no hay fecha", () => {
  assert.equal(formatearFecha("2026-12-04"), "04/12/2026");
  assert.equal(formatearFecha(null), "—");
});

test("textoMotivoMulta: etiquetas en español", () => {
  assert.equal(textoMotivoMulta("DANO"), "Daño");
  assert.equal(textoMotivoMulta("PERDIDA"), "Pérdida");
  assert.equal(textoMotivoMulta("RETRASO"), "Retraso");
});

test("puedeCancelarReserva: solo pendientes y listas para recoger", () => {
  assert.equal(puedeCancelarReserva({ estado: "PENDIENTE" }), true);
  assert.equal(puedeCancelarReserva({ estado: "LISTA" }), true);
  for (const estado of ["ATENDIDA", "CANCELADA", "VENCIDA"]) {
    assert.equal(puedeCancelarReserva({ estado }), false);
  }
});

test("textoSituacionReserva: qué le toca a la persona", () => {
  assert.match(textoSituacionReserva({ estado: "LISTA", fechaLimiteRecogida: "2026-12-04", codigoEjemplar: "EJ-001" }), /recogerlo.*EJ-001.*04\/12\/2026/);
  assert.match(textoSituacionReserva({ estado: "LISTA", fechaLimiteRecogida: "2026-12-04" }), /recogerlo.*04\/12\/2026/);
  assert.match(textoSituacionReserva({ estado: "PENDIENTE", posicionCola: 2, fechaVencimiento: "2026-12-04" }), /lugar 2 de la cola.*04\/12\/2026/);
  assert.equal(textoSituacionReserva({ estado: "PENDIENTE", fechaVencimiento: "2026-12-04" }), "Vence el 04/12/2026.");
  assert.equal(textoSituacionReserva({ estado: "CANCELADA" }), "");
});

test("librosReservables: con ejemplares pero ninguno disponible, y activos", () => {
  const libros = [
    { idLibro: 1, estado: "ACTIVO", totalEjemplares: 2, ejemplaresDisponibles: 0 },
    { idLibro: 2, estado: "ACTIVO", totalEjemplares: 2, ejemplaresDisponibles: 1 },
    { idLibro: 3, estado: "ACTIVO", totalEjemplares: 0, ejemplaresDisponibles: 0 },
    { idLibro: 4, estado: "INACTIVO", totalEjemplares: 1, ejemplaresDisponibles: 0 },
  ];
  assert.deepEqual(librosReservables(libros).map((l) => l.idLibro), [1]);
  assert.deepEqual(librosReservables(undefined), []);
});

test("textoMultasPendientes: vacío si no debe nada", () => {
  assert.equal(textoMultasPendientes(0), "");
  assert.equal(textoMultasPendientes(null), "");
  assert.match(textoMultasPendientes(5000), /multas pendientes/);
});
