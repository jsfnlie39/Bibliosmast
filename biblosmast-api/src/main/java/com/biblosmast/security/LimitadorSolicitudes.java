package com.biblosmast.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limita cuántas veces se puede pedir algo en una ventana de tiempo (por ejemplo, registros desde una misma IP).
 * Vive en la memoria de la API: al reiniciarla se borra. Basta para este proyecto; con varios servidores
 * habría que guardarlo en la base de datos o en un almacén compartido.
 */
@Component
public class LimitadorSolicitudes {

    private final Map<String, Deque<Long>> solicitudes = new ConcurrentHashMap<>();

    /** True si todavía se permite (y la cuenta como una solicitud); false si ya superó el máximo en la ventana. */
    public boolean permitir(String clave, int maximo, Duration ventana) {
        long ahora = System.currentTimeMillis();
        long limite = ahora - ventana.toMillis();
        Deque<Long> cola = solicitudes.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst() < limite) {
                cola.pollFirst();
            }
            if (cola.size() >= maximo) {
                return false;
            }
            cola.addLast(ahora);
            return true;
        }
    }
}
