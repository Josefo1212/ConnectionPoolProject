import java.sql.Connection;

void main() {
    try {
        var cliente = new Cliente(5); // Número de peticiones configurable
        IO.println("--- Simulación SIN pool de conexiones ---");
        cliente.ejecutarSinPool();
        IO.println("\n--- Simulación CON pool de conexiones ---");
        cliente.ejecutarConPool();
    } catch (Exception e) {
        System.out.println("Error en main: " + e.getMessage());
    }
}
