// Interfaz para desacoplar Pool y PoolManager
import java.sql.Connection;

interface ConnectionPool {
    Connection getConnection() throws InterruptedException;
    void releaseConnection(Connection connection);
}

