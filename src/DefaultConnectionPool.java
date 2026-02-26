import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DefaultConnectionPool implements ConnectionPool {
    private static final int POOL_SIZE = Config.getInt("POOL_SIZE");
    private final BlockingQueue<Connection> pool;

    public DefaultConnectionPool() {
        pool = new ArrayBlockingQueue<>(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                    Config.get("DB_USER"),
                    Config.get("DB_PASSWORD")
                );
                pool.offer(conn);
            } catch (SQLException e) {
                // Si falla una conexión, simplemente no la agrega
            }
        }
    }

    @Override
    public Connection getConnection() throws InterruptedException {
        return pool.take();
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            pool.offer(connection);
        }
    }
}
