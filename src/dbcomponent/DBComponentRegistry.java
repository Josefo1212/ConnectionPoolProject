package dbcomponent;

import adapters.DatabaseType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro simple para mantener 1 DBComponent por tipo de BD.
 *
 * La UI conecta y guarda el componente aquí; luego la simulación lo consume.
 */
public final class DBComponentRegistry {
    private static final ConcurrentHashMap<DatabaseType, DBComponent> COMPONENTS = new ConcurrentHashMap<>();

    private DBComponentRegistry() {
    }

    public static void put(DatabaseType type, DBComponent component) {
        if (type == null) throw new IllegalArgumentException("type no puede ser null");
        if (component == null) throw new IllegalArgumentException("component no puede ser null");
        COMPONENTS.put(type, component);
    }

    public static DBComponent get(DatabaseType type) {
        return COMPONENTS.get(type);
    }

    public static boolean isConnected(DatabaseType type) {
        return COMPONENTS.containsKey(type);
    }
}

