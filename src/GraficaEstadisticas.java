import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.Node;

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
        barChart.setPrefHeight(400);
        barChart.setPrefWidth(700);
        barChart.setStyle("-fx-background-color: #23243a; -fx-font-size: 15px;");
        this.getChildren().add(barChart);
        this.setSpacing(10);
        this.setStyle("-fx-alignment: center; -fx-padding: 20;");
    }

    public void mostrarResultados(int exitosasSinPool, int fallidasSinPool, int exitosasConPool, int fallidasConPool) {
        barChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.getData().add(new XYChart.Data<>("Sin pool - Exitosas", exitosasSinPool));
        serie.getData().add(new XYChart.Data<>("Sin pool - Fallidas", fallidasSinPool));
        serie.getData().add(new XYChart.Data<>("Con pool - Exitosas", exitosasConPool));
        serie.getData().add(new XYChart.Data<>("Con pool - Fallidas", fallidasConPool));
        barChart.getData().add(serie);

        // Colores personalizados
        for (int i = 0; i < serie.getData().size(); i++) {
            final int idx = i;
            final XYChart.Data<String, Number> data = serie.getData().get(i);
            final String color;
            switch (idx) {
                case 0: color = "#7c3aed"; break; // Sin pool - Exitosas
                case 1: color = "#ff4b4b"; break; // Sin pool - Fallidas
                case 2: color = "#b96eff"; break; // Con pool - Exitosas
                case 3: color = "#ffb347"; break; // Con pool - Fallidas
                default: color = "#a88ff0";
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
                        }
                    });
                }
            });
        }
    }
}
