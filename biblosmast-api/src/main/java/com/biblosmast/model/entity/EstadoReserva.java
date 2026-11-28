package com.biblosmast.model.entity;

/**
 * PENDIENTE: en la cola, esperando un ejemplar.
 * LISTA: ya hay un ejemplar apartado para el usuario; tiene hasta fechaLimiteRecogida para recogerlo.
 * ATENDIDA / CANCELADA: cerradas.
 * VENCIDA: una PENDIENTE con la fecha pasada se calcula al consultar; una LISTA que no se recogió a tiempo queda guardada así.
 */
public enum EstadoReserva { PENDIENTE, LISTA, ATENDIDA, CANCELADA, VENCIDA }
