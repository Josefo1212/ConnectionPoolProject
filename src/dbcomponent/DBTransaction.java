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
     * Debe liberar recursos (ej. devolver la Connection al pool).
     * Implementaciones suelen hacer rollback si queda activa y no se hizo commit.
     */
    @Override
    void close() throws DBException;
}
