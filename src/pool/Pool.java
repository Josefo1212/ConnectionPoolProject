package pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

class Pool implements ConnectionPool {
    private static Pool instance;
    private static final Object lock = new Object();
    private final ArrayBlockingQueue<Connection> connectionPool;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private Pool() throws SQLException {
        this(
                resolveDbUrl(),
                Config.get("DB_USER"),
                Config.get("DB_PASSWORD"),
                Config.getInt("POOL_SIZE")
        );
    }

    private Pool(String dbUrl, String dbUser, String dbPassword, int poolSize) throws SQLException {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);

        loadDriver();

        for (var i = 0; i < poolSize; i++) {
            connectionPool.add(createConnection());
        }
    }

    static Pool getInstance() throws SQLException {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Pool();
                }
            }
        }
        return instance;
    }

    private static void loadDriver() throws SQLException {
        // Preferido: DB_DRIVER en .env (permite alternar PostgreSQL/MySQL sin tocar código)
        String driver = Config.get("DB_DRIVER");

        // Fallback: si no existe, asumimos PostgreSQL para no romper el proyecto actual.
        if (driver == null || driver.isBlank()) {
            driver = "org.postgresql.Driver";
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver JDBC: " + driver, e);
        }
    }

    private static String resolveDbUrl() {
        // Preferido: DB_URL (ej: jdbc:postgresql://... o jdbc:mysql://...)
        String url = Config.get("DB_URL");
        if (url != null && !url.isBlank()) return url;

        // Fallback (compatibilidad con la config actual de PostgreSQL)
        return "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME");
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
