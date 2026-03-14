package adapters.postgres;

/**
 * Configuración fija del adapter de PostgreSQL.
 *
 * Nota: ahora la configuración específica (host/port/db/user/pass/driver/url)
 * vive aquí y NO en el .env, para poder soportar múltiples adapters.
 */
public final class PostgreSQLConfig {
    private PostgreSQLConfig() {
    }

    // Driver JDBC
    public static final String DRIVER = "org.postgresql.Driver";

    // Datos de conexión (ajusta según tu entorno PostgreSQL)
    public static final String HOST = "localhost";
    public static final int PORT = 5433;
    public static final String DB_NAME = "javaprueba";
    public static final String USER = "postgres";
    public static final String PASSWORD = "jf121206";

    public static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB_NAME;
}
