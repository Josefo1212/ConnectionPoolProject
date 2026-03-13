package adapters.postgres;

import dbcomponent.DBConnection;
import dbcomponent.DBException;
import pool.PoolManager;

import java.sql.Connection;

public class PostgreSQLConnection implements DBConnection {
    private final Connection connection;
    private final PoolManager poolManager;
    private boolean released = false;

    public PostgreSQLConnection(Connection connection, PoolManager poolManager) {
        this.connection = connection;
        this.poolManager = poolManager;
    }

    @Override
    public void connect() throws DBException {
        // N/A: la conexión ya viene abierta desde el pool.
    }

    @Override
    public void disconnect() throws DBException {
        if (released) return;
        try {
            released = true;
            poolManager.releaseConnection(connection);
        } catch (Exception e) {
            throw new DBException("Error al liberar conexión al pool", e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public Connection getRawConnection() {
        return connection;
    }
}
