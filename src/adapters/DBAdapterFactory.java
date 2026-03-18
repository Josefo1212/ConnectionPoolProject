package adapters;

import adapters.mysql.MySQLAdapter;
import adapters.postgres.PostgreSQLAdapter;

public final class DBAdapterFactory {
    private DBAdapterFactory() {
    }


    /**
     * Devuelve el adapter (metadatos driver/url/queries) para ser usado por DBComponent.
     */
    public static IDBAdapter adapter(DatabaseType type) {
        if (type == null) throw new IllegalArgumentException("DatabaseType no puede ser null");
        return switch (type) {
            case POSTGRES -> new PostgreSQLAdapter();
            case MYSQL -> new MySQLAdapter();
        };
    }
}

