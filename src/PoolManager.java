import java.sql.Connection;

public class PoolManager {
    private final ConnectionPool pool;

    public PoolManager() throws Exception {
        pool = Pool.getInstance(); // Usa la interfaz ConnectionPool
    }

    public Connection getConnection() throws Exception {
        return pool.getConnection();
    }

    public void releaseConnection(Connection connection) {
        pool.releaseConnection(connection);
    }
}
