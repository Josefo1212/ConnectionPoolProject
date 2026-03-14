package pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

class Pool implements ConnectionPool {
    private static volatile Pool instance;
    private static final Object lock = new Object();

    private final ArrayBlockingQueue<Connection> connectionPool;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private Pool(String driverClassName, String dbUrl, String dbUser, String dbPassword, int poolSize) throws SQLException {
        this.dbUrl = Objects.requireNonNull(dbUrl, "dbUrl");
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);

        loadDriver(driverClassName);

        for (var i = 0; i < poolSize; i++) {
            connectionPool.add(createConnection());
        }
    }

    /**
     * Inicializa el singleton del pool una sola vez.
     *
     * Debe llamarse ANTES del primer getInstance() (idealmente desde Main/adapter factory).
     */
    static void initialize(String driverClassName, String dbUrl, String dbUser, String dbPassword) throws SQLException {
        if (instance != null) return;
        synchronized (lock) {
            if (instance != null) return;
            int poolSize = Config.getInt("POOL_SIZE");
            instance = new Pool(driverClassName, dbUrl, dbUser, dbPassword, poolSize);
        }
    }

    static Pool getInstance() throws SQLException {
        var local = instance;
        if (local == null) {
            throw new SQLException("Pool no inicializado. Llama a Pool.initialize(...) antes de usar PoolManager.");
        }
        return local;
    }

    private static void loadDriver(String driver) throws SQLException {
        if (driver == null || driver.isBlank()) {
            throw new SQLException("Driver JDBC vacío. El adapter debe proveer driverClassName.");
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver JDBC: " + driver, e);
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    @Override
    public Connection getConnection() throws InterruptedException {
        var timeout = Config.getLong("POOL_TIMEOUT");
        return connectionPool.poll(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            connectionPool.offer(connection);
        }
    }

    void closePool() throws SQLException {
        for (var connection : connectionPool) {
            connection.close();
        }
    }
}
