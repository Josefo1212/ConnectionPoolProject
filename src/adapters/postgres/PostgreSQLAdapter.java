package adapters.postgres;

import dbcomponent.*;
import pool.PoolManager;

public class PostgreSQLAdapter implements IDBComponent<Object> {
    private final PoolManager poolManager;

    public PostgreSQLAdapter() {
        this.poolManager = PoolManager.getInstance();
    }

    @Override
    public DBQueryResult<Object> executeQuery(String query) throws DBException {
        // Implementar lógica usando connection del pool
        return null;
    }

    @Override
    public DBQueryResult<Object> executeUpdate(String query) throws DBException {
        // Implementar lógica usando connection del pool
        return null;
    }

    @Override
    public DBConnection getConnection() throws DBException {
        // Retornar un PostgreSQLConnection
        return new PostgreSQLConnection(poolManager.getConnection());
    }

    @Override
    public DBTransaction transaction() throws DBException {
        // Retornar implementación de transacción
        return new PostgresSQLTransaction(poolManager.getConnection());
    }

    @Override
    public DBQueryBatch batch() throws DBException {
        // Retornar implementación de batch
        return new PostgreSQLBatch(poolManager.getConnection());
    }

    @Override
    public DBQueryFile queryFiles() throws DBException {
        // Retornar implementación de queryfile
        return new PostgresSQLQueryFile(poolManager.getConnection());
    }

    @Override
    public void close() throws DBException {
        // Cerrar recursos si aplica
    }
}

