import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Interfaz extends Application {
    @Override
    public void start(Stage stage) {
        var btnSimular = new Button("Iniciar simulación");
        var btnFreno = new Button("Freno de emergencia");
        var lblPeticiones = new Label("Número de peticiones:");
        var txtPeticiones = new TextField();
        var statsBox = new VBox();
        var statsLabel = new Label("Estadísticas");
        var statsSinPool = new Label();
        var statsConPool = new Label();
        var progressBarSinPool = new ProgressBar(0);
        progressBarSinPool.setPrefWidth(400);
        var progressBarConPool = new ProgressBar(0);
        progressBarConPool.setPrefWidth(400);
        var progresoSinPool = new Label();
        var progresoConPool = new Label();
        statsBox.getChildren().addAll(statsLabel, progressBarSinPool, progresoSinPool, statsSinPool, progressBarConPool, progresoConPool, statsConPool);

        var titulo = new Label("Pool de Conexiones - Simulación");
        var controls = new HBox(btnSimular, btnFreno);
        controls.setSpacing(15);
        var peticionesBox = new HBox(lblPeticiones, txtPeticiones);
        peticionesBox.setSpacing(10);
        var layout = new VBox(titulo, peticionesBox, controls, statsBox);
        layout.setSpacing(15);
        layout.setStyle("-fx-alignment: center; -fx-padding: 30 0 0 0;");
        // CSS embebido mejorado y suavizado
        var css = """
            .root {
                -fx-background-color: linear-gradient(to bottom, #23243a 0%, #2d2e4a 100%);
                -fx-font-family: 'Segoe UI', 'Arial', sans-serif;
            }
            .titulo {
                -fx-font-size: 26px;
                -fx-text-fill: #a88ff0;
                -fx-font-weight: bold;
                -fx-padding: 0 0 18 0;
                -fx-alignment: center;
            }
            .label {
                -fx-font-size: 16px;
                -fx-text-fill: #a88ff0;
                -fx-font-weight: bold;
                -fx-alignment: center-left;
            }
            .input {
                -fx-background-color: #292a3a;
                -fx-text-fill: #e0e0f0;
                -fx-border-color: #a88ff0;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-font-size: 15px;
                -fx-padding: 7 12 7 12;
            }
            .boton {
                -fx-background-color: linear-gradient(to bottom, #a88ff0 0%, #7c6ee6 100%);
                -fx-text-fill: #23243a;
                -fx-font-size: 16px;
                -fx-font-weight: bold;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-padding: 10 28 10 28;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, #a88ff0, 8, 0.2, 0, 2);
                -fx-alignment: center;
            }
            .boton:hover {
                -fx-background-color: linear-gradient(to bottom, #b6aaff 0%, #a88ff0 100%);
                -fx-text-fill: #23243a;
            }
            .area {
                -fx-control-inner-background: #23243a;
                -fx-text-fill: #a88ff0;
                -fx-font-size: 14px;
                -fx-border-color: #a88ff0;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-effect: dropshadow(gaussian, #a88ff0, 8, 0.2, 0, 2);
            }
            .stats-box {
                -fx-background-color: #23243a;
                -fx-padding: 18;
                -fx-border-radius: 14;
                -fx-background-radius: 14;
                -fx-effect: dropshadow(gaussian, #a88ff0, 8, 0.2, 0, 2);
                -fx-alignment: center;
            }
            .stats-label {
                -fx-font-size: 20px;
                -fx-text-fill: #a88ff0;
                -fx-font-weight: bold;
                -fx-alignment: center;
            }
            .stats {
                -fx-text-fill: #e0e0f0;
                -fx-font-size: 15px;
                -fx-padding: 6 0 6 0;
                -fx-alignment: center;
            }
            .progress-bar {
                -fx-accent: #a88ff0;
                -fx-background-radius: 10;
                -fx-border-radius: 10;
                -fx-pref-height: 18px;
            }
        """;

        // Aplica las clases CSS a los nodos
        titulo.getStyleClass().add("titulo");
        lblPeticiones.getStyleClass().add("label");
        txtPeticiones.getStyleClass().add("input");
        btnSimular.getStyleClass().add("boton");
        btnFreno.getStyleClass().add("boton");
        statsBox.getStyleClass().add("stats-box");
        statsLabel.getStyleClass().add("stats-label");
        statsSinPool.getStyleClass().add("stats");
        statsConPool.getStyleClass().add("stats");
        progressBarSinPool.getStyleClass().add("progress-bar");
        progressBarConPool.getStyleClass().add("progress-bar");
        progresoSinPool.setStyle("-fx-alignment: center; -fx-text-fill: #b6aaff; -fx-font-size: 13px;");
        progresoConPool.setStyle("-fx-alignment: center; -fx-text-fill: #b6aaff; -fx-font-size: 13px;");
        layout.setStyle("-fx-alignment: center;");
        statsBox.setStyle("-fx-alignment: center;");
        controls.setStyle("-fx-alignment: center;");
        peticionesBox.setStyle("-fx-alignment: center;");

        // Instancia de la gráfica
        var grafica = new GraficaEstadisticas();
        grafica.setVisible(false);
        grafica.setPrefHeight(320);
        grafica.setMaxHeight(320);

        // Layout principal reorganizado
        var mainBox = new VBox(
            titulo,
            peticionesBox,
            controls,
            statsBox,
            grafica
        );
        mainBox.setSpacing(20);
        mainBox.setStyle("-fx-alignment: center; -fx-padding: 20 0 0 0;");
        var scene = new Scene(mainBox, 900, 700);

        // Escribir el CSS en un archivo temporal y cargarlo
        try {
            java.nio.file.Path tempCss = java.nio.file.Files.createTempFile("estilo", ".css");
            java.nio.file.Files.writeString(tempCss, css);
            scene.getStylesheets().add(tempCss.toUri().toString());
        } catch (Exception e) {
            System.err.println("No se pudo aplicar el CSS: " + e.getMessage());
        }
        stage.setScene(scene);
        stage.setTitle("Pool de Conexiones - Simulación");
        stage.show();


        btnSimular.setOnAction(_ -> {
            Platform.runLater(() -> {
                statsSinPool.setText("");
                statsConPool.setText("");
                progresoSinPool.setText("");
                progresoConPool.setText("");
                progressBarSinPool.setProgress(0);
                progressBarConPool.setProgress(0);
                grafica.setVisible(false);
                statsBox.setVisible(true);
            });
            new Thread(() -> {
                Cliente.activarFreno(false);
                int num;
                try { num = Integer.parseInt(txtPeticiones.getText()); }
                catch (NumberFormatException ex) {
                    Platform.runLater(() -> statsSinPool.setText("Número inválido"));
                    return;
                }
                var colaSin = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
                var managerSin = new EstadisticaManager(colaSin);
                var hiloSin = new Thread(managerSin);
                var cliente = new Cliente(num);
                cliente.setEstadisticaQueue(colaSin);
                hiloSin.start();
                Thread actualizador = new Thread(() -> {
                    while (cliente.getCompletadas() < num && !Cliente.estaFrenado()) {
                        double progreso = cliente.getCompletadas() / (double) num;
                        int completadas = cliente.getCompletadas();
                        int faltantes = num - completadas;
                        Platform.runLater(() -> {
                            progressBarSinPool.setProgress(progreso);
                            progresoSinPool.setText("Completadas: " + completadas + " | Faltantes: " + faltantes);
                            statsBox.setVisible(true);
                            grafica.setVisible(false);
                        });
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                    Platform.runLater(() -> progressBarSinPool.setProgress(1.0));
                });
                actualizador.setDaemon(true);
                actualizador.start();
                cliente.ejecutarSinPoolConEstadisticas();
                managerSin.stop();
                try { hiloSin.join(); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    int exitosas = managerSin.getExitosas();
                    int fallidas = managerSin.getFallidas();
                    double exitoPct = managerSin.getPorcentajeExito();
                    double falloPct = managerSin.getPorcentajeFallo();
                    statsSinPool.setText(
                        "✅ Sin pool: " + exitosas + " exitosas, " + fallidas + " fallidas, " +
                        String.format("%.2f", exitoPct) + "% éxito, " +
                        String.format("%.2f", falloPct) + "% fallo"
                    );
                    progresoSinPool.setText("");
                });
                Cliente.activarFreno(false);
                var colaCon = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
                var managerCon = new EstadisticaManager(colaCon);
                var hiloCon = new Thread(managerCon);
                var clientePool = new Cliente(num);
                clientePool.setEstadisticaQueue(colaCon);
                hiloCon.start();
                Thread actualizador2 = new Thread(() -> {
                    while (clientePool.getCompletadas() < num && !Cliente.estaFrenado()) {
                        double progreso = clientePool.getCompletadas() / (double) num;
                        int completadas = clientePool.getCompletadas();
                        int faltantes = num - completadas;
                        Platform.runLater(() -> {
                            progressBarConPool.setProgress(progreso);
                            progresoConPool.setText("Completadas: " + completadas + " | Faltantes: " + faltantes);
                            statsBox.setVisible(true);
                            grafica.setVisible(false);
                        });
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                    Platform.runLater(() -> progressBarConPool.setProgress(1.0));
                });
                actualizador2.setDaemon(true);
                actualizador2.start();
                clientePool.ejecutarConPoolConEstadisticas();
                managerCon.stop();
                try { hiloCon.join(); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    int exitosas = managerCon.getExitosas();
                    int fallidas = managerCon.getFallidas();
                    double exitoPct = managerCon.getPorcentajeExito();
                    double falloPct = managerCon.getPorcentajeFallo();
                    statsConPool.setText(
                        "✨ Con pool: " + exitosas + " exitosas, " + fallidas + " fallidas, " +
                        String.format("%.2f", exitoPct) + "% éxito, " +
                        String.format("%.2f", falloPct) + "% fallo"
                    );
                    progresoConPool.setText("");
                });
                // Al terminar ambas tandas, mostrar la gráfica
                Platform.runLater(() -> {
                    grafica.mostrarResultados(
                        managerSin.getExitosas(),
                        managerSin.getFallidas(),
                        managerCon.getExitosas(),
                        managerCon.getFallidas()
                    );
                    grafica.setVisible(true);
                });
            }).start();
        });
        btnFreno.setOnAction(_ -> {
            Cliente.activarFreno(true);
            Platform.runLater(() -> statsSinPool.setText("Freno de emergencia activado"));
        });
    }
}
