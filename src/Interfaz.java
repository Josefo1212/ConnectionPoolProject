import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import adapters.DBAdapterFactory;
import adapters.DatabaseType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Interfaz extends Application {
    private final VBox panelGrafica = new VBox();

    private final Label statsSinPool = new Label("0% Éxito");
    private final Label statsConPool = new Label("0% Éxito");

    private final ProgressBar progressSin = new ProgressBar(0);
    private final ProgressBar progressCon = new ProgressBar(0);

    private TextField txtPeticiones;
    private RadioButton rbPostgres, rbMySql;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Progreso objetivo (lo que reporta la simulación)
    private final DoubleProperty targetProgresoSin = new SimpleDoubleProperty(0);
    private final DoubleProperty targetProgresoCon = new SimpleDoubleProperty(0);

    // Progreso mostrado (se interpola para verse fluido)
    private final DoubleProperty shownProgresoSin = new SimpleDoubleProperty(0);
    private final DoubleProperty shownProgresoCon = new SimpleDoubleProperty(0);

    private AnimationTimer smoothTimer;

    private Label errorMsg = new Label("");

    @Override
    public void start(Stage stage) {
        // --- CONTENEDOR PRINCIPAL (HBox para dividir en 2 columnas) ---
        HBox root = new HBox(25);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-root");

        // ================= COLUMNA IZQUIERDA (Configuración) =================
        VBox leftCol = new VBox(20);
        // Evitar que se encoja cuando aparece la gráfica
        leftCol.setMinWidth(350);
        leftCol.setPrefWidth(350);
        leftCol.setMaxWidth(350);
        HBox.setHgrow(leftCol, Priority.NEVER);

        leftCol.getStyleClass().add("panel-oscuro");
        leftCol.setAlignment(Pos.TOP_CENTER);

        Label titleLeft = new Label("Configuración de\nParámetros");
        titleLeft.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLeft.setTextFill(Color.WHITE);
        titleLeft.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Selector DB
        ToggleGroup tgDb = new ToggleGroup();
        rbPostgres = new RadioButton("PostgreSQL");
        rbMySql = new RadioButton("MySQL");
        rbPostgres.setToggleGroup(tgDb);
        rbMySql.setToggleGroup(tgDb);
        rbPostgres.getStyleClass().add("db-selector");
        rbMySql.getStyleClass().add("db-selector");
        HBox dbContainer = new HBox(15, rbPostgres, rbMySql);
        dbContainer.setAlignment(Pos.CENTER);

        Label lblDb = new Label("Base de Datos");
        lblDb.setTextFill(Color.LIGHTGRAY);

        // Control Peticiones
        Label lblPet = new Label("Número de Peticiones");
        lblPet.setTextFill(Color.LIGHTGRAY);
        txtPeticiones = new TextField("0");
        txtPeticiones.getStyleClass().add("custom-field");

        // Botones Acción
        Button btnSimular = new Button("▶ Iniciar");
        btnSimular.getStyleClass().add("btn-iniciar");
        btnSimular.setMaxWidth(Double.MAX_VALUE);

        Button btnFreno = new Button("■ Alto de emergencia");
        btnFreno.getStyleClass().add("btn-freno");
        btnFreno.setMaxWidth(Double.MAX_VALUE);

        errorMsg.setTextFill(Color.web("#ff4e8e"));
        errorMsg.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        errorMsg.setWrapText(true);
        errorMsg.setMaxWidth(Double.MAX_VALUE);
        errorMsg.setAlignment(Pos.CENTER);
        errorMsg.setPadding(new Insets(8, 0, 0, 0));
        leftCol.getChildren().addAll(titleLeft, dbContainer, lblDb, lblPet, txtPeticiones, btnSimular, btnFreno, errorMsg);

        // ================= COLUMNA DERECHA (Métricas) =================
        VBox rightCol = new VBox(20);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.getStyleClass().add("panel-metriz");
        rightCol.setPadding(new Insets(20));

        Label titleRight = new Label("Métricas de Rendimiento en Tiempo Real");
        titleRight.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleRight.setTextFill(Color.web("#00ffff"));

        // Bindings de progreso (ahora se bindea al progreso 'shown' para suavizado)
        progressSin.progressProperty().bind(shownProgresoSin);
        progressCon.progressProperty().bind(shownProgresoCon);

        // Tarjetas KPI
        HBox kpiBox = new HBox(15);
        VBox cardSin = crearTarjetaKPI("Sin Pool", statsSinPool, progressSin, "#ff9966");
        VBox cardCon = crearTarjetaKPI("Con Pool", statsConPool, progressCon, "#00ffff");
        HBox.setHgrow(cardSin, Priority.ALWAYS);
        HBox.setHgrow(cardCon, Priority.ALWAYS);
        kpiBox.getChildren().addAll(cardSin, cardCon);

        // Área de Gráficas
        panelGrafica.setAlignment(Pos.CENTER);
        VBox.setVgrow(panelGrafica, Priority.ALWAYS);

        rightCol.getChildren().addAll(titleRight, kpiBox, panelGrafica);
        root.getChildren().addAll(leftCol, rightCol);

        Scene scene = new Scene(root, 1000, 650);
        aplicarCSS(scene);

        stage.setTitle("Simulación Pool de Conexiones Pro");
        stage.setScene(scene);
        stage.show();

        startSmoothProgressAnimation();

        btnSimular.setOnAction(_ -> ejecutarSimulacion());
        btnFreno.setOnAction(_ -> {
            Cliente.activarFreno(true);
            errorMsg.setText("");
            Platform.runLater(() -> statsSinPool.setText("Freno de emergencia activado"));
        });

        stage.setOnCloseRequest(_ -> {
            try {
                scheduler.shutdownNow();
            } catch (Exception ignored) {
            }
            if (smoothTimer != null) smoothTimer.stop();
        });
    }

    private void startSmoothProgressAnimation() {
        if (smoothTimer != null) smoothTimer.stop();

        smoothTimer = new AnimationTimer() {
            // factor de suavizado (más alto = más rápido hacia el target)
            private static final double ALPHA = 0.18;

            @Override
            public void handle(long now) {
                smoothStep(shownProgresoSin, targetProgresoSin, ALPHA);
                smoothStep(shownProgresoCon, targetProgresoCon, ALPHA);
            }

            private void smoothStep(DoubleProperty shown, DoubleProperty target, double alpha) {
                double s = shown.get();
                double t = clamp01(target.get());
                double next = s + (t - s) * alpha;

                // snap al final para evitar quedarse "temblando" cerca de 1
                if (Math.abs(t - next) < 0.002) next = t;
                shown.set(clamp01(next));
            }

            private double clamp01(double v) {
                if (v < 0) return 0;
                if (v > 1) return 1;
                return v;
            }
        };

        smoothTimer.start();
    }

    private VBox crearTarjetaKPI(String titulo, Label val, ProgressBar pb, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 15; -fx-border-color: " + color + "; -fx-border-radius: 15;");
        card.setMinWidth(220);
        card.setPrefWidth(260);
        card.setMaxWidth(420); // más ancho para texto largo

        Label t = new Label(titulo);
        t.setTextFill(Color.LIGHTGRAY);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        t.setWrapText(true);
        t.setMaxWidth(Double.MAX_VALUE);
        t.setAlignment(Pos.CENTER);

        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        val.setTextFill(Color.web(color));
        val.setWrapText(true);
        val.setMaxWidth(400); // más ancho para texto largo
        val.setMinWidth(220);
        val.setAlignment(Pos.CENTER);

        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: " + color + ";");

        card.getChildren().addAll(t, val, pb);
        return card;
    }

    private void aplicarCSS(Scene scene) {
        String style = """
            .main-root { -fx-background-color: #0d1117; }
            .panel-oscuro { -fx-background-color: #161b22; -fx-background-radius: 20; -fx-padding: 25; }
            .panel-metriz { -fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-radius: 20; -fx-border-width: 2; }
            .custom-field { -fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d; -fx-border-radius: 5; -fx-alignment: center; -fx-font-size: 18; }
            .btn-iniciar { -fx-background-color: linear-gradient(to bottom, #1f6feb, #0969da); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12; }
            .btn-freno { -fx-background-color: #8b0000; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 12; }
            .db-selector { -fx-text-fill: white; }
        """;

        scene.getStylesheets().add("data:text/css," + style.replace("\n", ""));
    }

    private void ejecutarSimulacion() {
        // Validar DB seleccionada
        DatabaseType selected;
        errorMsg.setText("");
        if (rbPostgres.isSelected()) selected = DatabaseType.POSTGRES;
        else if (rbMySql.isSelected()) selected = DatabaseType.MYSQL;
        else {
            errorMsg.setText("Selecciona PostgreSQL o MySQL antes de simular");
            return;
        }

        // Configurar DB para Cliente
        Cliente.setDatabaseType(selected);

        // Inicializar pool vía adapter ANTES de correr simulaciones.
        try {
            DBAdapterFactory.create(selected);
        } catch (Exception e) {
            Platform.runLater(() -> statsSinPool.setText("Error inicializando adapter/pool: " + e.getMessage()));
            return;
        }

        // Reset UI
        Platform.runLater(() -> {
            statsSinPool.setText("0% Éxito");
            statsConPool.setText("0% Éxito");
            targetProgresoSin.set(0);
            targetProgresoCon.set(0);
            panelGrafica.getChildren().clear();
        });

        new Thread(() -> {
            Cliente.activarFreno(false);

            int num;
            try {
                num = Integer.parseInt(txtPeticiones.getText());
            } catch (NumberFormatException ex) {
                Platform.runLater(() -> statsSinPool.setText("Número inválido"));
                return;
            }
            if (num <= 0) {
                Platform.runLater(() -> statsSinPool.setText("Ingresa un número mayor a 0"));
                return;
            }

            // ================= SIN POOL =================
            var colaSin = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
            var managerSin = new EstadisticaManager(colaSin);
            var hiloSin = new Thread(managerSin);
            var cliente = new Cliente(num);
            cliente.setEstadisticaQueue(colaSin);

            final boolean[] terminadoSinPool = {false};
            final ScheduledFuture<?>[] futureSin = new ScheduledFuture<?>[1];
            futureSin[0] = scheduler.scheduleAtFixedRate(() -> {
                int completadas = cliente.getCompletadas();
                double progreso = completadas / (double) num;
                Platform.runLater(() -> {
                    targetProgresoSin.set(Math.min(progreso, 1.0));
                    statsSinPool.setText("Sin pool: " + completadas + "/" + num + " | faltan " + Math.max(0, num - completadas));
                });
                if (completadas >= num || Cliente.estaFrenado()) {
                    Platform.runLater(() -> targetProgresoSin.set(1.0));
                    terminadoSinPool[0] = true;
                    if (futureSin[0] != null) futureSin[0].cancel(false);
                }
            }, 0, 20, TimeUnit.MILLISECONDS);

            hiloSin.start();
            cliente.ejecutarSinPoolConEstadisticas();
            managerSin.stop();
            try { hiloSin.join(); } catch (InterruptedException ignored) {}
            while (!terminadoSinPool[0]) {
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            }

            // Mostrar KPIs sin pool (éxitos/fallos + %)
            Platform.runLater(() -> {
                int ex = managerSin.getExitosas();
                int fa = managerSin.getFallidas();
                double exitoPct = managerSin.getPorcentajeExito();
                statsSinPool.setText(String.format("Sin pool: %d ok / %d fail | %.2f%% éxito", ex, fa, exitoPct));
            });

            // ================= CON POOL =================
            Cliente.activarFreno(false);
            var colaCon = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
            var managerCon = new EstadisticaManager(colaCon);
            var hiloCon = new Thread(managerCon);
            var clientePool = new Cliente(num);
            clientePool.setEstadisticaQueue(colaCon);

            final boolean[] terminadoConPool = {false};
            final ScheduledFuture<?>[] futureCon = new ScheduledFuture<?>[1];
            futureCon[0] = scheduler.scheduleAtFixedRate(() -> {
                int completadas = clientePool.getCompletadas();
                double progreso = completadas / (double) num;
                Platform.runLater(() -> {
                    targetProgresoCon.set(Math.min(progreso, 1.0));
                    statsConPool.setText("Con pool: " + completadas + "/" + num + " | faltan " + Math.max(0, num - completadas));
                });
                if (completadas >= num || Cliente.estaFrenado()) {
                    Platform.runLater(() -> targetProgresoCon.set(1.0));
                    terminadoConPool[0] = true;
                    if (futureCon[0] != null) futureCon[0].cancel(false);
                }
            }, 0, 20, TimeUnit.MILLISECONDS);

            hiloCon.start();
            clientePool.ejecutarConPoolConEstadisticas();
            managerCon.stop();
            try { hiloCon.join(); } catch (InterruptedException ignored) {}
            while (!terminadoConPool[0]) {
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            }

            Platform.runLater(() -> {
                int ex = managerCon.getExitosas();
                int fa = managerCon.getFallidas();
                double exitoPct = managerCon.getPorcentajeExito();
                statsConPool.setText(String.format("Con pool: %d ok / %d fail | %.2f%% éxito", ex, fa, exitoPct));
            });

            // Gráficas
            Platform.runLater(() -> {
                mostrarGraficaEstadisticas(
                    managerSin.getExitosas(),
                    managerSin.getFallidas(),
                    managerCon.getExitosas(),
                    managerCon.getFallidas()
                );
            });
        }).start();
    }

    private void mostrarGraficaEstadisticas(int exitosasSinPool, int fallidasSinPool, int exitosasConPool, int fallidasConPool) {
        panelGrafica.getChildren().clear();
        VBox grafica = new GraficaEstadisticas(exitosasSinPool, fallidasSinPool, exitosasConPool, fallidasConPool).crearGrafica();
        panelGrafica.getChildren().add(grafica);
    }
}
