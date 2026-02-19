import java.sql.Connection;

public class PoolManager {
    private final Pool pool;

    public PoolManager() throws Exception {
        pool = Pool.getInstance(); // Usa el singleton
    }

    public Connection getConnection() throws Exception {
        return pool.getConnection();
    }

    public void releaseConnection(Connection connection) {
        pool.releaseConnection(connection);
    }
}
