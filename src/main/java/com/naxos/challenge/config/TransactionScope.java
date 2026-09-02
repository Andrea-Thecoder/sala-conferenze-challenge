package com.naxos.challenge.config;

import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * UserTransaction non implementa AutoCloseable di suo: questo wrapper glielo
 * dà, così begin()/rollback() possono stare in un try-with-resources — stessa
 * ergonomia di Transaction (Closeable) in Ebean.
 *
 * commit() va chiamato esplicitamente a fine try: se non viene chiamato
 * (perché è stata lanciata un'eccezione prima), close() fa rollback in automatico.
 */
public final class TransactionScope implements AutoCloseable {

    private final UserTransaction userTransaction;
    private boolean committed = false;

    public TransactionScope(UserTransaction userTransaction) throws Exception {
        this.userTransaction = userTransaction;
        userTransaction.begin();
    }

    public void commit() throws Exception {
        userTransaction.commit();
        committed = true;
    }

    @Override
    public void close() throws SystemException {
        if (!committed) {
            try {
                userTransaction.rollback();
            } catch (IllegalStateException | SecurityException e) {
                // transazione già chiusa/non nello stato giusto: niente da fare
            }
        }
    }
}
