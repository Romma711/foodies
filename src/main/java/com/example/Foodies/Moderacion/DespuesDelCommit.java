package com.example.Foodies.Moderacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Posterga una acción hasta que la transacción actual se confirma. Se usa para los emails de
 * confirmación: así nadie recibe el aviso de algo que después se revirtió, y el envío no deja la
 * transacción abierta mientras espera al servidor de correo. Un error de la acción se registra en el
 * log y nunca llega a la respuesta: la operación ya quedó guardada.
 */
final class DespuesDelCommit {

    private static final Logger log = LoggerFactory.getLogger(DespuesDelCommit.class);

    private DespuesDelCommit() {
    }

    /**
     * @param accion lo que se ejecuta tras el commit; si no hay transacción activa, se ejecuta ya
     */
    static void ejecutar(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            ejecutarSinPropagar(accion);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ejecutarSinPropagar(accion);
            }
        });
    }

    private static void ejecutarSinPropagar(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException ex) {
            log.error("Falló una acción posterior al commit: {}", ex.getMessage(), ex);
        }
    }
}
