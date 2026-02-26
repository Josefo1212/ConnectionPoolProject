import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

public class GraficaEstadisticas extends VBox {
    private final BarChart<String, Number> barChart;
    private final CategoryAxis xAxis;
    private final NumberAxis yAxis;

    public GraficaEstadisticas() {
        xAxis = new CategoryAxis();
        yAxis = new NumberAxis();
        xAxis.setLabel("Tipo");
        yAxis.setLabel("Cantidad");
        yAxis.setTickLabelFill(Color.web("#a88ff0"));
        xAxis.setTickLabelFill(Color.web("#a88ff0"));
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Resultados de la Simulación");
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(320);
        barChart.setPrefWidth(700);
        barChart.setStyle("-fx-background-color: #23243a; -fx-font-size: 15px;");
        this.getChildren().add(barChart);
        this.setSpacing(18);
        this.setStyle("-fx-alignment: center; -fx-padding: 20;");
    }

    public void mostrarResultados(int exitosasSinPool, int fallidasSinPool, int exitosasConPool, int fallidasConPool) {
        barChart.getData().clear();
        // Configuro el eje de categorías para mostrar siempre las cuatro
        xAxis.setCategories(javafx.collections.FXCollections.observableArrayList(
            "Sin pool - Exitosas",
            "Sin pool - Fallidas",
            "Con pool - Exitosas",
            "Con pool - Fallidas"
        ));
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        // Evito valores cero para que siempre se rendericen
        serie.getData().add(new XYChart.Data<>("Sin pool - Exitosas", exitosasSinPool > 0 ? exitosasSinPool : 0.01));
        serie.getData().add(new XYChart.Data<>("Sin pool - Fallidas", fallidasSinPool > 0 ? fallidasSinPool : 0.01));
        serie.getData().add(new XYChart.Data<>("Con pool - Exitosas", exitosasConPool > 0 ? exitosasConPool : 0.01));
        serie.getData().add(new XYChart.Data<>("Con pool - Fallidas", fallidasConPool > 0 ? fallidasConPool : 0.01));
        barChart.getData().add(serie);

        // Colores personalizados y etiquetas debajo de cada barra
        for (int i = 0; i < serie.getData().size(); i++) {
            final int idx = i;
            final XYChart.Data<String, Number> data = serie.getData().get(i);
            final String color;
            final String etiqueta;
            switch (idx) {
                case 0: color = "#7c3aed"; etiqueta = "Sin pool - Exitosas"; break;
                case 1: color = "#ff4b4b"; etiqueta = "Sin pool - Fallidas"; break;
                case 2: color = "#b96eff"; etiqueta = "Con pool - Exitosas"; break;
                case 3: color = "#ffb347"; etiqueta = "Con pool - Fallidas"; break;
                default: color = "#a88ff0"; etiqueta = "";
            }
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + color + ";");
                    // Etiqueta de porcentaje
                    final int total = (idx < 2) ? (exitosasSinPool + fallidasSinPool) : (exitosasConPool + fallidasConPool);
                    double pct = total > 0 ? data.getYValue().doubleValue() * 100.0 / total : 0;
                    Text label = new Text(String.format("%.1f%%", pct));
                    label.setFill(Color.web(color));
                    label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                    newNode.parentProperty().addListener((o, oldParent, newParent) -> {
                        if (newParent != null) {
                            ((javafx.scene.Group)newParent).getChildren().add(label);
                            label.layoutXProperty().bind(newNode.layoutXProperty().add(20));
                            label.layoutYProperty().bind(newNode.layoutYProperty().subtract(10));
                            // Etiqueta debajo de la barra
                            Text barraLabel = new Text(etiqueta);
                            barraLabel.setFill(Color.web(color));
                            barraLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-opacity: 0.85;");
                            ((javafx.scene.Group)newParent).getChildren().add(barraLabel);
                            barraLabel.layoutXProperty().bind(newNode.layoutXProperty().add(10));
                            barraLabel.layoutYProperty().bind(newNode.layoutYProperty().add(40));
                        }
                    });
                }
            });
        }
    }
}
