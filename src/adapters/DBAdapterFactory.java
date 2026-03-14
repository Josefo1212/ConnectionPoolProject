package adapters;

import adapters.mysql.MySQLAdapter;
import adapters.postgres.PostgreSQLAdapter;
import dbcomponent.IDBComponent;

public final class DBAdapterFactory {
    private DBAdapterFactory() {
    }

    public static IDBComponent<Object> create(DatabaseType type) {
        if (type == null) throw new IllegalArgumentException("DatabaseType no puede ser null");
        return switch (type) {
            case POSTGRES -> new PostgreSQLAdapter();
            case MYSQL -> new MySQLAdapter();
        };
    }
}

