package dbcomponent;

import adapters.DBAdapterFactory;
import adapters.DatabaseType;
import adapters.IDBAdapter;

/**
 * Servicio de conexión para desacoplar la UI de la lógica de creación del DBComponent.
 */
public final class DBComponentConnector {

    private static final DBQueryId DEFAULT_PING_QUERY = new DBQueryId("usuario.selectOne");

    public ConnectResult connect(DatabaseType type,
                                 String host,
                                 int port,
                                 String dbName,
                                 String user,
                                 String password) throws DBException {
        if (type == null) {
            throw new DBException(DBException.Category.CONFIG, null, "DatabaseType no puede ser null");
        }

        IDBAdapter adapter = DBAdapterFactory.adapter(type);
        ConnectionConfig cfg = adapter.toConnectionConfig(host, port, dbName, user, password);
        String queriesLocation = toClasspathLocation(adapter.queriesResource());

        DBComponent component = new DBComponent(
                cfg.driverClassName(),
                cfg.url(),
                cfg.user(),
                cfg.password(),
                queriesLocation
        );

        // Verificación temprana de conectividad y queries predefinidas.
        component.query(DEFAULT_PING_QUERY);

        return new ConnectResult(type, cfg, queriesLocation, component);
    }

    private String toClasspathLocation(String adapterResource) {
        if (adapterResource == null || adapterResource.isBlank()) {
            throw new IllegalArgumentException("queriesResource no puede ser null/vacío");
        }
        String normalized = adapterResource.startsWith("/") ? adapterResource : "/" + adapterResource;
        return "classpath:" + normalized;
    }

    public record ConnectResult(
            DatabaseType type,
            ConnectionConfig config,
            String queriesLocation,
            DBComponent component
    ) {
    }
}

