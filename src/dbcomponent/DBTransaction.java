package dbcomponent;

/**
 * Contrato genérico para manejar transacciones.
 * La implementación concreta depende del motor (ej. PostgreSQL).
 */
public interface DBTransaction extends AutoCloseable {
    void begin() throws DBException;

    void commit() throws DBException;

    void rollback() throws DBException;

    /**
     * Indica si la transacción sigue activa (begin() ya fue llamado y aún no se hizo commit/rollback).
     */
    default boolean isActive() {
        return false;
    }

    /**
     * Debe liberar recursos (ej. devolver la Connection al pool).
     * Implementaciones suelen hacer rollback si queda activa y no se hizo commit.
     */
    @Override
    void close() throws DBException;
}
