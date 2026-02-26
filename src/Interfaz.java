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
        var statsBox = new VBox(10);
        statsBox.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 15; -fx-border-radius: 10; -fx-background-radius: 10;");
        var statsLabel = new Label("Estadísticas");
        statsLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #b96eff; -fx-font-weight: bold;");
        var statsSinPool = new Label();
        var statsConPool = new Label();
        var progresoLabel = new Label();
        progresoLabel.setStyle("-fx-text-fill: #b96eff; -fx-font-size: 15px; -fx-font-weight: bold;");
        statsSinPool.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px;");
        statsConPool.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px;");
        // Barra de progreso
        var progressBarSinPool = new ProgressBar(0);
        progressBarSinPool.setPrefWidth(400);
        var progressBarConPool = new ProgressBar(0);
        progressBarConPool.setPrefWidth(400);
        // Etiquetas de progreso en tiempo real
        var progresoSinPool = new Label();
        progresoSinPool.setStyle("-fx-text-fill: #b96eff; -fx-font-size: 14px;");
        var progresoConPool = new Label();
        progresoConPool.setStyle("-fx-text-fill: #b96eff; -fx-font-size: 14px;");
        // Agregar solo una vez cada elemento, en el orden correcto
        statsBox.getChildren().clear();
        statsBox.getChildren().addAll(statsLabel, progressBarSinPool, progresoSinPool, statsSinPool, progressBarConPool, progresoConPool, statsConPool);

        // Hoja de estilos CSS embebida
        var css = """
            .root {
                -fx-background-color: #181028;
                -fx-font-family: 'Segoe UI', 'Arial', sans-serif;
            }
            .titulo {
                -fx-font-size: 22px;
                -fx-text-fill: #b96eff;
                -fx-font-weight: bold;
                -fx-padding: 0 0 10 0;
            }
            .label {
                -fx-font-size: 15px;
                -fx-text-fill: #b96eff;
                -fx-font-weight: bold;
            }
            .input {
                -fx-background-color: #222;
                -fx-text-fill: #fff;
                -fx-border-color: #b96eff;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-font-size: 14px;
                -fx-padding: 6 10 6 10;
            }
            .boton {
                -fx-background-color: #b96eff;
                -fx-text-fill: #181028;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-border-radius: 8;
                -fx-padding: 8 20 8 20;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, #b96eff, 8, 0.2, 0, 2);
            }
            .boton:hover {
                -fx-background-color: #7c3aed;
                -fx-text-fill: #fff;
            }
            .area {
                -fx-control-inner-background: #222;
                -fx-text-fill: #b96eff;
                -fx-font-size: 13px;
                -fx-border-color: #b96eff;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-effect: dropshadow(gaussian, #b96eff, 8, 0.2, 0, 2);
            }
            .stats-box {
                -fx-background-color: #1a1a1a;
                -fx-padding: 15;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-effect: dropshadow(gaussian, #b96eff, 8, 0.2, 0, 2);
            }
            .stats-label {
                -fx-font-size: 18px;
                -fx-text-fill: #b96eff;
                -fx-font-weight: bold;
            }
            .stats {
                -fx-text-fill: #fff;
                -fx-font-size: 14px;
                -fx-padding: 4 0 4 0;
            }
        """;

        var titulo = new Label("Pool de Conexiones - Simulación");
        titulo.getStyleClass().add("titulo");

        lblPeticiones.getStyleClass().add("label");
        txtPeticiones.getStyleClass().add("input");
        btnSimular.getStyleClass().add("boton");
        btnFreno.getStyleClass().add("boton");
        statsBox.getStyleClass().add("stats-box");
        statsLabel.getStyleClass().add("stats-label");
        statsSinPool.getStyleClass().add("stats");
        statsConPool.getStyleClass().add("stats");

        var controls = new HBox(btnSimular, btnFreno);
        controls.setSpacing(15);
        controls.setStyle("-fx-alignment: center;");

        var peticionesBox = new HBox(lblPeticiones, txtPeticiones);
        peticionesBox.setSpacing(10);
        peticionesBox.setStyle("-fx-alignment: center-left;");

        // Eliminar el área de queries y mostrar solo las barras de progreso
        var layout = new VBox(titulo, peticionesBox, controls, statsBox);
        layout.setSpacing(15);
        layout.setStyle("-fx-padding: 30; -fx-background-color: #181028; -fx-border-radius: 12; -fx-background-radius: 12;");

        var scene = new Scene(layout, 600, 400);
        stage.setScene(scene);
        scene.getStylesheets().add("data:text/css," + css.replace("\n", "%0A"));
        stage.setTitle("Pool de Conexiones - Simulación");
        stage.show();

        btnSimular.setOnAction(_ -> {
            new Thread(() -> {
                Cliente.activarFreno(false); // Reinicia el freno antes de iniciar
                int num;
                try {
                    num = Integer.parseInt(txtPeticiones.getText());
                } catch (NumberFormatException ex) {
                    Platform.runLater(() -> statsSinPool.setText("Número inválido"));
                    return;
                }
                var colaSin = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
                var managerSin = new EstadisticaManager(colaSin);
                var hiloSin = new Thread(managerSin);
                var cliente = new Cliente(num);
                cliente.setEstadisticaQueue(colaSin);
                hiloSin.start();
                // Actualización de barra de progreso y progreso en tiempo real
                Thread actualizador = new Thread(() -> {
                    while (cliente.getCompletadas() < num && !Cliente.estaFrenado()) {
                        double progreso = cliente.getCompletadas() / (double) num;
                        int completadas = cliente.getCompletadas();
                        int faltantes = num - completadas;
                        Platform.runLater(() -> {
                            progressBarSinPool.setProgress(progreso);
                            progresoSinPool.setText("Completadas: " + completadas + " | Faltantes: " + faltantes);
                        });
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
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
                        });
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
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
            }).start();
        });
        btnFreno.setOnAction(_ -> {
            Cliente.activarFreno(true);
            Platform.runLater(() -> statsSinPool.setText("Freno de emergencia activado"));
        });
    }
}
