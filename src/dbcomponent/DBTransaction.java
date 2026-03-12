package dbcomponent;

/**
 * Contrato genérico para manejar transacciones.
 * La implementación concreta depende del motor (ej. PostgreSQL).
 */
public interface DBTransaction {
    void begin() throws DBException;

    void commit() throws DBException;

    void rollback() throws DBException;
}

