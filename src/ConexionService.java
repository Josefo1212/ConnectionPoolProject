import adapters.DatabaseType;
import dbcomponent.DBComponentConnector;
import dbcomponent.DBComponentRegistry;

/**
 * Servicio de conexion desacoplado de JavaFX.
 * La UI solo envia datos y pinta los resultados.
 */
public final class ConexionService {
    private final DBComponentConnector connector = new DBComponentConnector();

    public ConnectionView conectar(DatabaseType type,
                                   String host,
                                   int port,
                                   String dbName,
                                   String user,
                                   String password) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("Selecciona PostgreSQL o MySQL antes de conectar");
        }

        DBComponentRegistry.clear(type);
        DBComponentConnector.ConnectResult result = connector.connect(type, host, port, dbName, user, password);
        DBComponentRegistry.put(result.type(), result.component());

        return new ConnectionView(result.type(), result.config().url());
    }

    public void limpiarConexion(DatabaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Selecciona PostgreSQL o MySQL para limpiar su conexion");
        }
        DBComponentRegistry.clear(type);
    }

    public boolean isConnected(DatabaseType type) {
        if (type == null) return false;
        return DBComponentRegistry.isConnected(type);
    }

    public record ConnectionView(DatabaseType type, String url) {
    }
}




