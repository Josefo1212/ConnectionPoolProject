import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
        var areaQueries = new TextArea();
        var statsBox = new VBox(10);
        statsBox.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 15; -fx-border-radius: 10; -fx-background-radius: 10;");
        var statsLabel = new Label("Estadísticas");
        statsLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #b96eff; -fx-font-weight: bold;");
        var statsSinPool = new Label();
        var statsConPool = new Label();
        statsSinPool.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px;");
        statsConPool.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px;");
        statsBox.getChildren().addAll(statsLabel, statsSinPool, statsConPool);

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
        areaQueries.getStyleClass().add("area");
        statsBox.getStyleClass().add("stats-box");
        statsLabel.getStyleClass().add("stats-label");
        statsSinPool.getStyleClass().add("stats");
        statsConPool.getStyleClass().add("stats");

        areaQueries.setEditable(false);
        areaQueries.setPrefHeight(350);
        areaQueries.setPrefWidth(500);
        areaQueries.setPromptText("Aquí se mostrarán las queries ejecutadas...");

        var controls = new HBox(15, btnSimular, btnFreno);
        controls.setStyle("-fx-alignment: center;");

        var peticionesBox = new HBox(10, lblPeticiones, txtPeticiones);
        peticionesBox.setStyle("-fx-alignment: center-left;");

        var layout = new VBox(15, titulo, peticionesBox, controls, areaQueries, statsBox);
        layout.setStyle("-fx-padding: 30; -fx-background-color: #181028; -fx-border-radius: 12; -fx-background-radius: 12;");

        var scene = new Scene(layout, 600, 520);
        stage.setScene(scene);
        scene.getStylesheets().add("data:text/css," + css.replace("\n", "%0A"));
        stage.setTitle("Pool de Conexiones - Simulación");
        stage.show();

        btnSimular.setOnAction(_ -> {
            new Thread(() -> {
                Cliente.activarFreno(false); // Reinicia el freno antes de iniciar
                Platform.runLater(() -> areaQueries.appendText("\n--- Simulación SIN pool de conexiones ---\n"));
                int num;
                try {
                    num = Integer.parseInt(txtPeticiones.getText());
                } catch (NumberFormatException ex) {
                    Platform.runLater(() -> areaQueries.appendText("Número inválido\n"));
                    return;
                }
                var colaSin = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
                var managerSin = new EstadisticaManager(colaSin);
                var hiloSin = new Thread(managerSin);
                var cliente = new Cliente(num);
                cliente.setOutput(areaQueries);
                cliente.setEstadisticaQueue(colaSin);
                hiloSin.start();
                cliente.ejecutarSinPoolConEstadisticas();
                managerSin.stop();
                try { hiloSin.join(); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    statsSinPool.setText(
                        "✅ Sin pool: " + managerSin.getExitosas() + " exitosas, " + managerSin.getFallidas() + " fallidas, " +
                        String.format("%.2f", managerSin.getPorcentajeExito()) + "% éxito, " +
                        String.format("%.2f", managerSin.getPorcentajeFallo()) + "% fallo"
                    );
                });
                Cliente.activarFreno(false);
                Platform.runLater(() -> areaQueries.appendText("\n--- Simulación CON pool de conexiones ---\n"));
                var colaCon = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
                var managerCon = new EstadisticaManager(colaCon);
                var hiloCon = new Thread(managerCon);
                var clientePool = new Cliente(num);
                clientePool.setOutput(areaQueries);
                clientePool.setEstadisticaQueue(colaCon);
                hiloCon.start();
                clientePool.ejecutarConPoolConEstadisticas();
                managerCon.stop();
                try { hiloCon.join(); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    statsConPool.setText(
                        "✨ Con pool: " + managerCon.getExitosas() + " exitosas, " + managerCon.getFallidas() + " fallidas, " +
                        String.format("%.2f", managerCon.getPorcentajeExito()) + "% éxito, " +
                        String.format("%.2f", managerCon.getPorcentajeFallo()) + "% fallo"
                    );
                });
            }).start();
        });
        btnFreno.setOnAction(_ -> {
            Cliente.activarFreno(true);
            Platform.runLater(() -> areaQueries.appendText("Freno de emergencia activado\n"));
        });
    }
}
