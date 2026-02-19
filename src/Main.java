import java.sql.Connection;

void main() {
    try {
        var poolManager = new PoolManager();
        var connection = poolManager.getConnection();
        if (connection != null && connection.isValid(2)) {
            System.out.println("Conexión obtenida y válida.");
        } else {
            System.out.println("No se pudo obtener una conexión válida.");
        }
        poolManager.releaseConnection(connection);
    } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
    }
}
