package adapters;

import dbcomponent.ConnectionConfig;

public interface IDBAdapter {
    DatabaseType type();

    /**
     * Nombre de la clase driver JDBC.
     */
    String driverClassName();

    /**
     * Construye la URL JDBC a partir de datos ingresados por el usuario.
     */
    String buildJdbcUrl(String host, int port, String dbName);

    /**
     * Recurso classpath que contiene queries predefinidas para este adapter.
     */
    String queriesResource();

    /**
     * Crea un ConnectionConfig listo para instanciar un DBComponent.
     */
    default ConnectionConfig toConnectionConfig(String host, int port, String dbName, String user, String password) {
        return new ConnectionConfig(driverClassName(), buildJdbcUrl(host, port, dbName), user, password);
    }
}

