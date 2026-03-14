package pool;

import java.sql.Connection;

public class PoolManager {
    private static PoolManager instance;
    private final ConnectionPool pool;

    private PoolManager() {
        try {
            pool = Pool.getInstance();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo inicializar el pool de conexiones", e);
        }
    }

    /**
     * Inicializa el pool genérico con parámetros del adapter (driver/url/user/pass).
     * Debe ejecutarse antes de getInstance() la primera vez.
     */
    public static void initialize(String driverClassName, String url, String user, String password) {
        try {
            Pool.initialize(driverClassName, url, user, password);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo inicializar el pool con la configuración del adapter", e);
        }
    }

    public static synchronized PoolManager getInstance() {
        if (instance == null) instance = new PoolManager();
        return instance;
    }

    public Connection getConnection() {
        try {
            return pool.getConnection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void releaseConnection(Connection connection) {
        pool.releaseConnection(connection);
    }
}
