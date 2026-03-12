package dbcomponent;

/**
 * Excepción genérica para el componente de BD.
 *
 * Mantiene constructores simples (message / message+cause) para compatibilidad,
 * pero añade metadata útil (categoría/código) para diagnóstico.
 */
public class DBException extends Exception {

    public enum Category {
        CONNECTION,
        AUTH,
        TIMEOUT,
        SYNTAX,
        CONSTRAINT,
        TRANSACTION,
        IO,
        CONFIG,
        UNKNOWN
    }

    private final Category category;
    private final String errorCode;

    public DBException(String message) {
        this(Category.UNKNOWN, null, message, null);
    }

    public DBException(String message, Throwable cause) {
        this(Category.UNKNOWN, null, message, cause);
    }

    public DBException(Category category, String errorCode, String message) {
        this(category, errorCode, message, null);
    }

    public DBException(Category category, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.category = category == null ? Category.UNKNOWN : category;
        this.errorCode = errorCode;
    }

    public Category getCategory() {
        return category;
    }

    /**
     * Código específico del motor (ej. SQLSTATE en PostgreSQL) o un código propio.
     */
    public String getErrorCode() {
        return errorCode;
    }
}
