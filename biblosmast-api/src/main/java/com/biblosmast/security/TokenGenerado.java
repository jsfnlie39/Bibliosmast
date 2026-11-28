package com.biblosmast.security;

/** Resultado de crear un token: el texto del JWT y el momento en que vence (segundos desde 1970). */
public record TokenGenerado(String token, long expiraEnSegundos) {
}
