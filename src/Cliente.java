import java.sql.DriverManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class Cliente {
    private final int numeroPeticiones;
    private static final AtomicBoolean freno = new AtomicBoolean(false);
    private TextArea outputArea;
    private final ConcurrentLinkedQueue<String> mensajes = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<EstadisticaManager.Peticion> colaEstadisticas;
    private static final int MAX_THREADS = 100;
    private ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
    private static final int MAX_MENSAJES = 200;

    public Cliente(int numeroPeticiones) {
        this.numeroPeticiones = numeroPeticiones;
    }

    public void setOutput(TextArea area) {
        this.outputArea = area;
        iniciarActualizador();
    }

    private void print(String msg) {
        mensajes.offer(msg);
    }

    private void iniciarActualizador() {
        Platform.runLater(() -> {
            var hilo = new Thread(() -> {
                while (!estaFrenado()) {
                    if (!mensajes.isEmpty() && outputArea != null) {
                        Platform.runLater(() -> {
                            var buffer = new StringBuilder();
                            while (!mensajes.isEmpty()) {
                                buffer.append(mensajes.poll()).append("\n");
                            }
                            outputArea.appendText(buffer.toString());
                        });
                    }
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
            });
            hilo.setDaemon(true);
            hilo.start();
        });
    }

    // Métodos de estadísticas y freno
    public static void activarFreno(boolean estado) {
        freno.set(estado);
    }
    public static void activarFreno() {
        activarFreno(true);
    }
    public static boolean estaFrenado() {
        return freno.get();
    }
    Thread escucharFreno() {
        var t = new Thread(() -> {
            try {
                while (!estaFrenado()) {
                    if (System.in.available() > 0) {
                        int input = System.in.read();
                        if (input == '\n' || input == '\r') {
                            print("\nFreno de emergencia activado por el usuario. Deteniendo todas las peticiones...");
                            activarFreno();
                            break;
                        }
                    }
                    Thread.sleep(50);
                }
            } catch (Exception ignored) {}
        }, "FrenoThread");
        t.setDaemon(true);
        t.start();
        return t;
    }

    // Métodos principales de simulación con estadísticas
    public void setEstadisticaQueue(ConcurrentLinkedQueue<EstadisticaManager.Peticion> queue) {
        this.colaEstadisticas = queue;
    }

    public void ejecutarSinPoolConEstadisticas() {
        activarFreno(false);
        var frenoThread = escucharFreno();
        var inicio = System.currentTimeMillis();
        var tareas = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            tareas.add(executor.submit(() -> {
                boolean resultadoReportado = false;
                try (var connection = DriverManager.getConnection(
                        "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                        Config.get("DB_USER"),
                        Config.get("DB_PASSWORD")
                )) {
                    if (estaFrenado()) {
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada"));
                        resultadoReportado = true;
                        return;
                    }
                    try (var stmt = connection.createStatement()) {
                        var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                        boolean exito = false;
                        while (rs.next() && !estaFrenado()) {
                            exito = true;
                            print("[Sin pool] Petición " + idx + ": usuario = " + rs.getString(1));
                        }
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, exito, exito ? "OK" : "Sin resultados"));
                        resultadoReportado = true;
                    }
                    Thread.sleep(new Random().nextInt(500));
                } catch (Exception e) {
                    if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, e.getMessage()));
                    resultadoReportado = true;
                    print("Error en el cliente (sin pool), petición " + idx + ": " + e.getMessage());
                } finally {
                    if (!resultadoReportado && colaEstadisticas != null) {
                        colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada (final)"));
                    }
                }
            }));
        }
        for (var tarea : tareas) {
            try { tarea.get(); } catch (Exception ignored) {}
        }
        activarFreno(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        print("Tiempo total sin pool: " + (fin - inicio) + " ms");
    }

    public void ejecutarConPoolConEstadisticas() {
        activarFreno(false);
        var frenoThread = escucharFreno();
        var inicio = System.currentTimeMillis();
        var tareas = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            tareas.add(executor.submit(() -> {
                boolean resultadoReportado = false;
                try {
                    var poolManager = new PoolManager();
                    if (estaFrenado()) {
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada"));
                        resultadoReportado = true;
                        return;
                    }
                    var connection = poolManager.getConnection();
                    if (estaFrenado()) {
                        if (connection != null) poolManager.releaseConnection(connection);
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada"));
                        resultadoReportado = true;
                        return;
                    }
                    if (connection != null) {
                        try (var stmt = connection.createStatement()) {
                            var rs = stmt.executeQuery("SELECT * FROM usuario LIMIT 1");
                            boolean exito = false;
                            while (rs.next() && !estaFrenado()) {
                                exito = true;
                                print("[Con pool] Petición " + idx + ": usuario = " + rs.getString(1));
                            }
                            if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, exito, exito ? "OK" : "Sin resultados"));
                            resultadoReportado = true;
                        } finally {
                            poolManager.releaseConnection(connection);
                        }
                        Thread.sleep(new Random().nextInt(500));
                    } else if (!estaFrenado()) {
                        if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "No se pudo obtener conexión"));
                        resultadoReportado = true;
                        print("No se pudo obtener una conexión del pool en la petición " + idx);
                    }
                } catch (Exception e) {
                    if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, e.getMessage()));
                    resultadoReportado = true;
                    print("Error en el cliente (con pool), petición " + idx + ": " + e.getMessage());
                } finally {
                    if (!resultadoReportado && colaEstadisticas != null) {
                        colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "Frenada (final)"));
                    }
                }
            }));
        }
        for (var tarea : tareas) {
            try { tarea.get(); } catch (Exception ignored) {}
        }
        activarFreno(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        print("Tiempo total con pool: " + (fin - inicio) + " ms");
    }
}
