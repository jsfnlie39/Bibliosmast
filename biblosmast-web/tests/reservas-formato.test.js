import { test } from "node:test";
import assert from "node:assert/strict";

const {
  etiquetaEstadoReserva, claseEstadoReserva, textoTurno, opcionesEstadoEdicion, textoApartado,
} = await import("../js/reservas-formato.js");

test("etiquetaEstadoReserva: nombres en español y estado desconocido tal cual", () => {
  assert.equal(etiquetaEstadoReserva("PENDIENTE"), "En cola");
  assert.equal(etiquetaEstadoReserva("LISTA"), "Lista para recoger");
  assert.equal(etiquetaEstadoReserva("VENCIDA"), "Vencida");
  assert.equal(etiquetaEstadoReserva("RARO"), "RARO");
});

test("claseEstadoReserva: cada estado usa su color", () => {
  assert.equal(claseEstadoReserva("LISTA"), "lista");
  assert.equal(claseEstadoReserva("PENDIENTE"), "activo");
  assert.equal(claseEstadoReserva("VENCIDA"), "vencido");
  assert.equal(claseEstadoReserva("ATENDIDA"), "finalizado");
  assert.equal(claseEstadoReserva("RARO"), "finalizado");
});

test("textoTurno: turno en la cola o fecha y ejemplar a recoger", () => {
  assert.equal(textoTurno({ estado: "PENDIENTE", posicionCola: 2 }), "Turno 2");
  assert.equal(
    textoTurno({ estado: "LISTA", fechaLimiteRecogida: "2026-12-04", codigoEjemplar: "LIB-0002-A" }),
    "Recoger hasta 04/12/2026 · LIB-0002-A",
  );
  assert.equal(textoTurno({ estado: "LISTA", fechaLimiteRecogida: "2026-12-04" }), "Recoger hasta 04/12/2026");
  assert.equal(textoTurno({ estado: "PENDIENTE", posicionCola: null }), "—");
  assert.equal(textoTurno({ estado: "CANCELADA" }), "—");
  assert.equal(textoTurno(null), "—");
});

test("opcionesEstadoEdicion: una reserva lista solo se deja así o se cancela", () => {
  assert.deepEqual(opcionesEstadoEdicion("LISTA"), ["LISTA", "CANCELADA"]);
  assert.deepEqual(opcionesEstadoEdicion("PENDIENTE"), ["PENDIENTE", "ATENDIDA", "CANCELADA"]);
  assert.deepEqual(opcionesEstadoEdicion("VENCIDA"), ["PENDIENTE", "ATENDIDA", "CANCELADA"]);
});

test("textoApartado: avisa solo cuando el ejemplar quedó apartado", () => {
  assert.equal(
    textoApartado({ apartadoPara: "Laura Gomez" }),
    " El ejemplar quedó apartado para Laura Gomez, que ya puede recogerlo.",
  );
  assert.equal(textoApartado({ apartadoPara: null }), "");
  assert.equal(textoApartado({}), "");
  assert.equal(textoApartado(undefined), "");
});
