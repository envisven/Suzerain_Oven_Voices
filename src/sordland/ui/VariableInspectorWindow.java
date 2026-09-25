package sordland.ui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import sordland.analysis.*;
import sordland.data.Domain.Dataset;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

                                                                                                 
public final class VariableInspectorWindow {
    private final Stage stage = new Stage();
    private final BorderPane root = new BorderPane();
    private final VBox top = new VBox(8);
    private final Label status = new Label();
    private final Button back = new Button("← Back"), forward = new Button("Forward →"), cancel = new Button("Cancel");
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "variable-inspector");
        thread.setDaemon(true);
        return thread;
    });
    private final List<String> history = new ArrayList<>();
    private int cursor = -1;
    private long revision;
    private Task<?> active;
    private VariableIndex index;
    private VariableAnalyzer analyzer;
    public VariableInspectorWindow(Stage owner, Dataset data, Runnable closed) {
        stage.initOwner(owner);
        stage.setTitle("Variable Inspector");
        stage.setMinWidth(660);
        stage.setMinHeight(480);
        top.setPadding(new Insets(16));
        top.getChildren().add(new HBox(10, back, forward, cancel, status));
        root.setTop(top);
        root.setCenter(new Label("Preparing the variable catalog…"));
        back.setOnAction(event -> { if (cursor > 0) select(history.get(--cursor), false); });
        forward.setOnAction(event -> { if (cursor + 1 < history.size()) select(history.get(++cursor), false); });
        cancel.setOnAction(event -> {
            revision++;
            if (active != null) active.cancel(true);
            status.setText(index == null ? "Catalog cancelled. Close and reopen to retry." : "Analysis cancelled. Select a variable to continue.");
            cancel.setDisable(true);
        });
        stage.setOnHidden(event -> {
            revision++;
            if (active != null) active.cancel(true);
            worker.shutdownNow();
            closed.run();
        });
        Scene scene = new Scene(root, 1180, 780);
        Theme.apply(scene);
        stage.setScene(scene);
        updateHistory();
        stage.show();
        work("Indexing canonical source fields…", () -> new VariableIndex(data), result -> {
            index = result;
            analyzer = new VariableAnalyzer(index);
            top.getChildren().add(new VariablePicker(index, name -> select(name, true)));
            root.setCenter(new Label("Type to filter, then click a game variable. Scripts are never executed."));
            status.setText(index.variables().size() + " variables available");
        });
    }
    public void show() { stage.show(); stage.toFront(); }
    public void close() { stage.close(); }
    private void select(String name, boolean remember) {
        if (remember) {
            while (history.size() > cursor + 1) history.removeLast();
            history.add(name);
            cursor = history.size() - 1;
        }
        updateHistory();
        work("Analysing " + name + "…", () -> analyzer.analyze(name), result -> {
            ScrollPane scroll = new ScrollPane(new VariableAnalysisView(index, result, related -> select(related, true)));
            scroll.setFitToWidth(true);
            root.setCenter(scroll);
            status.setText("Source analysis complete");
        });
    }
    private void updateHistory() {
        back.setDisable(cursor <= 0);
        forward.setDisable(cursor + 1 >= history.size());
    }
    private <T> void work(String message, Supplier<T> operation, java.util.function.Consumer<T> done) {
        long request = ++revision;
        if (active != null) active.cancel(true);
        status.setText(message);
        cancel.setDisable(false);
        Task<T> task = new Task<>() { @Override protected T call() { return operation.get(); } };
        active = task;
        task.setOnSucceeded(event -> {
            if (request != revision) return;
            cancel.setDisable(true);
            done.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            if (request != revision) return;
            cancel.setDisable(true);
            status.setText("Unable to analyse: " + task.getException().getMessage());
        });
        worker.execute(task);
    }
}
