import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Pool {
    private final ArrayBlockingQueue<Connection> connectionPool; // Especificar el tipo genérico explícitamente
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public Pool() throws SQLException {
        this(
                "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                Config.get("DB_USER"),
                Config.get("DB_PASSWORD"),
                Config.getInt("POOL_SIZE")
        ); // Construye la URL dinámicamente desde el .env
    }

    public Pool(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        this(dbUrl, dbUser, dbPassword, Config.getInt("POOL_SIZE")); // Tamaño del pool desde el .env
    }

    public Pool(String dbUrl, String dbUser, String dbPassword, int poolSize) throws SQLException {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);
        try {
            Class.forName("org.postgresql.Driver"); // Carga explícita del driver
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de PostgreSQL", e);
        }
        for (int i = 0; i < poolSize; i++) {
            connectionPool.add(createConnection());
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public Connection getConnection() throws InterruptedException {
        long timeout = Config.getLong("POOL_TIMEOUT"); // Timeout desde el .env
        return connectionPool.poll(timeout, TimeUnit.MILLISECONDS);
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            connectionPool.offer(connection);
        }
    }

    public void closePool() throws SQLException {
        for (Connection connection : connectionPool) {
            connection.close();
        }
    }
}
