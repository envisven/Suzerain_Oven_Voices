package sordland.ui;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import sordland.analysis.*;
import java.util.function.Consumer;

                                                                              
public final class VariablePicker extends VBox {
    private final TextField query = new TextField();
    private final ListView<String> choices = new ListView<>();
    public VariablePicker(VariableIndex index, Consumer<String> selected) {
        super(6);
        var catalog = new VariableCatalog(index);
        query.setPromptText("Filter game variables… then click an exact option");
        query.setId("variable-query");
        choices.setId("variable-choices");
        choices.setPrefHeight(145);
        choices.setPlaceholder(new Label("No matching variables"));
        choices.setCellFactory(list -> {
            var cell = new ListCell<String>() {
                @Override protected void updateItem(String name, boolean empty) {
                    super.updateItem(name, empty);
                    setText(empty || name == null ? null : index.label(name).equals(name) ? name : index.label(name) + "\n" + name);
                }
            };
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) selected.accept(cell.getItem());
            });
            return cell;
        });
        query.textProperty().addListener((observable, previous, value) -> {
            choices.getItems().setAll(catalog.filter(value));
            choices.getSelectionModel().clearSelection();
            choices.setPrefHeight(Math.max(68, Math.min(145, 24 + choices.getItems().size() * 44)));
        });
        choices.getItems().setAll(catalog.filter(""));
        getChildren().addAll(query, choices);
    }
}
