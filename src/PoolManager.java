import java.sql.Connection;

public class PoolManager {
    private final Pool pool;

    public PoolManager() throws Exception {
        pool = new Pool(); // Usa los datos del .env vía Config
    }

    public Connection getConnection() throws Exception {
        return pool.getConnection();
    }

    public void releaseConnection(Connection connection) {
        pool.releaseConnection(connection);
    }
}
