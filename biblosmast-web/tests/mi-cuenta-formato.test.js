import { test } from "node:test";
import assert from "node:assert/strict";

const { librosReservables, resumenCuenta, avisoReservasListas } = await import("../js/mi-cuenta-formato.js");

const libro = (idLibro, titulo, total, disponibles, estado = "ACTIVO") =>
  ({ idLibro, titulo, totalEjemplares: total, ejemplaresDisponibles: disponibles, estado });

test("librosReservables: solo activos, con ejemplares y ninguno libre", () => {
  const libros = [
    libro(1, "Zorro", 2, 0),
    libro(2, "Álgebra", 1, 0),
    libro(3, "Con libre", 2, 1),
    libro(4, "Sin ejemplares", 0, 0),
    libro(5, "Inactivo", 1, 0, "INACTIVO"),
  ];
  assert.deepEqual(librosReservables(libros).map((l) => l.titulo), ["Álgebra", "Zorro"]);
});

test("librosReservables: no ofrece los libros que ya tiene reservados (en cola o listos)", () => {
  const libros = [libro(1, "Uno", 1, 0), libro(2, "Dos", 1, 0), libro(3, "Tres", 1, 0)];
  const reservas = [
    { idLibro: 1, estado: "PENDIENTE" },
    { idLibro: 2, estado: "LISTA" },
    { idLibro: 3, estado: "CANCELADA" },
  ];
  assert.deepEqual(librosReservables(libros, reservas).map((l) => l.idLibro), [3]);
});

test("resumenCuenta: cuenta lo que sigue abierto y suma las multas por pagar", () => {
  const r = resumenCuenta({
    prestamos: [{ estado: "ACTIVO" }, { estado: "VENCIDO" }, { estado: "FINALIZADO" }],
    reservas: [{ estado: "PENDIENTE" }, { estado: "LISTA" }, { estado: "VENCIDA" }, { estado: "ATENDIDA" }],
    multas: [{ estado: "PENDIENTE", valor: 3000 }, { estado: "PAGADA", valor: 9000 }, { estado: "PENDIENTE", valor: 10000 }],
  });
  assert.deepEqual(r, { prestamosActivos: 2, reservasAbiertas: 2, multasPendientes: 2, totalMultasPendientes: 13000 });
});

test("resumenCuenta: sin datos todo queda en cero", () => {
  assert.deepEqual(resumenCuenta({}), { prestamosActivos: 0, reservasAbiertas: 0, multasPendientes: 0, totalMultasPendientes: 0 });
});

test("avisoReservasListas: singular, plural y sin reservas listas", () => {
  assert.equal(avisoReservasListas([{ estado: "PENDIENTE", titulo: "A" }]), "");
  assert.equal(avisoReservasListas([{ estado: "LISTA", titulo: "Clean Code" }]),
    "Tienes un libro listo para recoger en la biblioteca: Clean Code.");
  assert.equal(avisoReservasListas([{ estado: "LISTA", titulo: "A" }, { estado: "LISTA", titulo: "B" }]),
    "Tienes 2 libros listos para recoger en la biblioteca: A, B.");
});
