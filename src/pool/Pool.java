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
                "jdbc:postgresql://" + pool.Config.get("DB_HOST") + ":" + pool.Config.get("DB_PORT") + "/" + pool.Config.get("DB_NAME"),
                pool.Config.get("DB_USER"),
                pool.Config.get("DB_PASSWORD"),
                pool.Config.getInt("POOL_SIZE")
        );
    }

    private Pool(String dbUrl, String dbUser, String dbPassword, int poolSize) throws SQLException {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de PostgreSQL", e);
        }
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

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    @Override
    public Connection getConnection() throws InterruptedException {
        var timeout = pool.Config.getLong("POOL_TIMEOUT");
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
