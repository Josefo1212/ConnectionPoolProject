void main() {
    try {
        // Indique el número de peticiones que hará el cliente
        var numeroPeticiones = 15000;
        var cliente = new Cliente(numeroPeticiones);
        IO.println("--- Simulación SIN pool de conexiones ---");
        cliente.ejecutarSinPool();
        IO.println("\n--- Simulación CON pool de conexiones ---");
        cliente.ejecutarConPool();
    } catch (Exception e) {
        System.out.println("Error en main: " + e.getMessage());
    }
}
