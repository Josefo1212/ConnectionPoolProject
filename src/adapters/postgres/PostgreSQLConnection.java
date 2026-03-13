package adapters.postgres;

import dbcomponent.DBConnection;
import dbcomponent.DBException;
import java.sql.Connection;

public class PostgreSQLConnection implements DBConnection {
    private final Connection connection;

    public PostgreSQLConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void connect() throws DBException {
        // Lógica de conexión si aplica
    }

    @Override
    public void disconnect() throws DBException {
        try {
            connection.close();
        } catch (Exception e) {
            throw new DBException("Error al cerrar conexión", e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public Connection getRawConnection() {
        return connection;
    }
}

