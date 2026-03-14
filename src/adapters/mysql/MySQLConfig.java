package adapters.mysql;

/**
 * Configuración fija del adapter de MySQL.
 *
 * Nota: la configuración específica (host/port/db/user/pass/driver/url)
 * vive aquí y NO en el .env.
 */
public final class MySQLConfig {
    private MySQLConfig() {
    }

    // Driver JDBC
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Datos docker que nos diste
    public static final String HOST = "localhost";
    public static final int PORT = 3307;
    public static final String DB_NAME = "javaprueba";
    public static final String USER = "josefo1212";
    public static final String PASSWORD = "jf121206";

    // URL (puedes agregar params más adelante: useSSL, serverTimezone, etc.)
    public static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME;
}

