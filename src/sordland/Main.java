package sordland;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sordland.data.*;
import sordland.data.Domain.*;
import sordland.graph.*;
import sordland.layout.LayoutEngine;
import sordland.ui.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;


public final class Main extends Application {
    private Stage stage;private BorderPane root;private StackPane center;private VBox top;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"sordland-model");t.setDaemon(true);return t;});
    private Dataset data;private View current,campaign;private final Deque<View> history=new ArrayDeque<>();
    private final Label status=new Label("Loading source data…"),notice=new Label(),title=new Label("Sordland campaign");
    private final TextField search=new TextField();private final ComboBox<String> type=new ComboBox<>(),turn=new ComboBox<>();
    private final CheckBox ancillary=new CheckBox("Ancillary data"),speakerColors=new CheckBox("Speaker Colors");
    private final Button back=new Button("← Back"),itemDetails=new Button("Item details"),eventView=new Button("Event view: ROOTED");private HBox filters;
    private boolean rootedCampaign=true;private int smokeStage;private View smokeReturnView;
    private final List<Button> zoomButtons=new ArrayList<>();
    private final ActorFilterControl actorFilter=new ActorFilterControl();
    private Task<?> activeTask;
    private final Map<String,javafx.scene.paint.Color> speakerPalette=new HashMap<>();
    private final Set<String> campaignExpanded=new HashSet<>();
    private VBox inspector;private Path dataDir;private String smokeDir;private static boolean smokeMode;private static volatile int smokeExitCode=2;private long revision;private boolean initializing;
    private static final String STYLE="-fx-font-family: 'System'; -fx-font-size: 13px; -fx-base: #f7f8f5; -fx-accent: #296a52; -fx-focus-color: #296a52;";
    private static final class View {
        final GraphCanvas canvas;final Graph graph;final boolean campaign;final String name;final Item item;final javafx.scene.Node detail;
        final Set<String> expanded;
        final List<String> actors;final Set<String> selectedActors=new LinkedHashSet<>();
        String query="",lastSearch="",campaignType="All types",campaignTurn="All turns";boolean ancillaryItems;int matchIndex=-1;
        View(GraphCanvas canvas,Graph graph,boolean campaign,String name,Item item,javafx.scene.Node detail,Set<String> expanded){this.canvas=canvas;this.graph=graph;this.campaign=campaign;this.name=name;this.item=item;this.detail=detail;this.expanded=expanded;actors=graph!=null&&!campaign?ActorProjection.actors(graph):List.of();selectedActors.addAll(actors);}
    }
    @Override public void start(Stage stage){
        this.stage=stage;dataDir=Path.of(System.getProperty("suzerain.data","data")).toAbsolutePath();
        for(String arg:getParameters().getRaw()){if(arg.startsWith("--data="))dataDir=Path.of(arg.substring(7)).toAbsolutePath();if(arg.startsWith("--smoke="))smokeDir=arg.substring(8);}
        root=new BorderPane();root.setStyle(STYLE);center=new StackPane();root.setCenter(center);
        top=new VBox();top.setStyle("-fx-background-color: #fcfdf9; -fx-border-color: transparent transparent #d5ddd4 transparent;");
        Label brand=new Label("SUZERAIN  /  SORDLAND");brand.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #516756;");
        Label offline=new Label("OFFLINE SOURCE EXPLORER");offline.setStyle("-fx-font-size: 10px; -fx-text-fill: #64826a;");
        Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button diagnostics=new Button("Data notes");diagnostics.setOnAction(e->diagnostics());
        Button files=new Button("Data files…");files.setOnAction(e->load(true));
        HBox mast=new HBox(12,brand,spacer,offline,files,diagnostics);mast.setAlignment(Pos.CENTER_LEFT);mast.setPadding(new Insets(12,20,9,20));
        back.setOnAction(e->goBack());back.setVisible(false);back.setManaged(false);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #223f2e;");title.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(title,Priority.ALWAYS);
        Button minus=new Button("−"),plus=new Button("+"),fit=new Button("Fit"),readable=new Button("Readable");
        minus.setOnAction(e->{if(current!=null&&current.canvas!=null)current.canvas.zoom(1/1.25);});plus.setOnAction(e->{if(current!=null&&current.canvas!=null)current.canvas.zoom(1.25);});
        fit.setOnAction(e->{if(current!=null&&current.canvas!=null)current.canvas.fit();});readable.setOnAction(e->{if(current!=null&&current.canvas!=null)current.canvas.actualSize();});
        speakerColors.setOnAction(e->{if(current!=null&&current.canvas!=null)current.canvas.setSpeakerColors(speakerColors.isSelected());});speakerColors.setVisible(false);speakerColors.setManaged(false);
        zoomButtons.addAll(List.of(minus,plus,fit,readable));
        itemDetails.setOnAction(e->{if(current!=null&&current.item!=null){history.push(current);show(new View(null,null,false,current.name,current.item,detail(current.item),Set.of()));}});
        eventView.setOnAction(e->{if(current!=null&&current.campaign){rootedCampaign=!isRooted(current);updateTurnChoices();rebuildCampaign();}});
        HBox toolbar=new HBox(9,back,title,eventView,itemDetails,minus,plus,fit,readable,speakerColors);toolbar.setAlignment(Pos.CENTER_LEFT);toolbar.setPadding(new Insets(0,20,12,20));
        search.setPromptText("Find title or database name…");search.setPrefWidth(340);search.setOnAction(e->search());
        Button find=new Button("Find");find.setOnAction(e->search());
        type.setPrefWidth(170);turn.setPrefWidth(150);type.setOnAction(e->{if(!initializing){if(isRooted(current)){current.campaignType=type.getValue();current.canvas.setCampaignFilters(search.getText(),type.getValue());}else rebuildCampaign();}});turn.setOnAction(e->{if(!initializing)rebuildCampaign();});ancillary.setOnAction(e->rebuildCampaign());
        filters=new HBox(10,search,find,type,turn,ancillary,actorFilter);filters.setAlignment(Pos.CENTER_LEFT);filters.setPadding(new Insets(0,20,12,20));
        notice.setWrapText(true);notice.setStyle("-fx-font-size: 11px; -fx-text-fill: #637467;");notice.setPadding(new Insets(0,20,11,20));
        top.getChildren().addAll(mast,toolbar,filters,notice);root.setTop(top);
        status.setPadding(new Insets(8,20,8,20));status.setStyle("-fx-background-color: #fcfdf9; -fx-text-fill: #586e60;");status.setMaxWidth(Double.MAX_VALUE);root.setBottom(status);
        Scene scene=new Scene(root,1440,920);stage.setTitle("Sordland Tree Viewer");stage.setScene(scene);stage.setMinWidth(1050);stage.setMinHeight(620);stage.show();
        load(false);
    }
    private void load(boolean chooseFiles){
        Path entity=dataDir.resolve(Loader.ENTITY_FILE),conversations=dataDir.resolve(Loader.CONVERSATIONS_FILE);
        if(chooseFiles||!Files.isRegularFile(entity))entity=choose("Select the entity catalogue",Loader.ENTITY_FILE);
        if(entity==null){if(data==null)loadCancelled();return;}
        if(chooseFiles||!Files.isRegularFile(conversations))conversations=choose("Select Sordland conversations",Loader.CONVERSATIONS_FILE);
        if(conversations==null){if(data==null)loadCancelled();return;}
        Path ep=entity,cp=conversations;
        runWork("Reading and validating Sordland source files…",()->Loader.load(ep,cp),loaded->{
            data=loaded;speakerPalette.clear();var speakers=new TreeSet<String>();
            data.conversations().values().forEach(c->c.entries().values().stream().filter(e->!e.isPlayer()&&!e.isNarrator()).forEach(e->speakers.add(e.speaker())));
            int speakerIndex=0;for(String name:speakers)speakerPalette.put(name,javafx.scene.paint.Color.hsb((speakerIndex++*137.50776405003785)%360,.19,.98));
            initializing=true;var types=new TreeSet<String>();data.items().forEach(i->types.add(i.type()));data.ancillary().forEach(i->types.add(i.type()));type.getItems().setAll("All types");type.getItems().addAll(types);type.setValue("All types");
            turn.setValue("All turns");initializing=false;updateTurnChoices();rebuildCampaign();
        });
    }
    private Path choose(String caption,String name){FileChooser chooser=new FileChooser();chooser.setTitle(caption+" ("+name+")");chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON files","*.json"),new FileChooser.ExtensionFilter("All files","*"));if(Files.isDirectory(dataDir))chooser.setInitialDirectory(dataDir.toFile());var file=chooser.showOpenDialog(stage);return file==null?null:file.toPath();}
    private void loadCancelled(){Label text=new Label("Both source JSON files are needed to open the viewer.");Button retry=new Button("Choose files again");retry.setOnAction(e->load(true));VBox panel=new VBox(16,text,retry);panel.setAlignment(Pos.CENTER);center.getChildren().setAll(panel);status.setText("Data loading cancelled");}
    private static boolean isRooted(View view){return view!=null&&view.campaign&&view.graph.campaign!=null;}
    private void updateTurnChoices(){
        if(data==null)return;String selected=turn.getValue();initializing=true;
        var turns=new TreeSet<Integer>();
        if(rootedCampaign)data.gameFlow().turns().forEach(t->turns.add(t.turnNumber()));
        else data.items().forEach(i->{if(i.turn()!=null)turns.add(i.turn());});
        turn.getItems().setAll("All turns");turns.forEach(t->turn.getItems().add("Turn "+t));
        if(!rootedCampaign)turn.getItems().add("Unspecified");
        turn.setValue(turn.getItems().contains(selected)?selected:"All turns");initializing=false;
    }
    private void rebuildCampaign(){
        if(data==null)return;String q=search.getText();String selectedType=type.getValue(),selectedTurn=turn.getValue();boolean extra=ancillary.isSelected(),rooted=rootedCampaign;
        Integer t=selectedTurn!=null&&selectedTurn.startsWith("Turn ")?Integer.valueOf(selectedTurn.substring(5)):null;
        runWork(rooted?"Laying out GameFlow progression…":"Laying out catalogue items…",()->{
            Graph graph=rooted?new RootedCampaignGraphBuilder().build(data,t):new CampaignGraphBuilder().build(data,q,selectedType,t,extra);
            if(!rooted&&"Unspecified".equals(selectedTurn))graph=new Graph(graph.title,graph.nodes.stream().filter(n->n.turn==null).toList(),graph.edges,graph.diagnostics);
            return new LayoutEngine().campaign(graph,campaignExpanded,new TextMeasurer());
        },layout->{
            var canvas=new GraphCanvas();View view=new View(canvas,layout.graph,true,"Sordland campaign",null,null,campaignExpanded);configure(view);canvas.setResult(layout,true);view.query=q;view.campaignType=selectedType;view.campaignTurn=selectedTurn;view.ancillaryItems=extra;campaign=view;history.clear();show(view);
            if(rooted)canvas.setCampaignFilters(q,selectedType);
            Platform.runLater(()->{if(!layout.boxes.isEmpty())canvas.focus(initialNode(layout));if(smokeDir!=null)smokeCampaign();});
        });
    }
    private String initialNode(LayoutEngine.Result layout){
        if(layout.graph.campaign!=null)return layout.graph.nodes.getFirst().id;
        return layout.graph.nodes.stream().filter(n->n.item!=null&&Boolean.TRUE.equals(Json.object(n.item.raw().get("ConversationProperties")).get("IsOnStart")))
            .map(n->n.id).findFirst().orElseGet(()->layout.graph.nodes.getFirst().id);
    }
    private void search(){
        if(current==null){rebuildCampaign();return;}
        if(current.campaign){
            if(!isRooted(current)){rebuildCampaign();return;}
            String q=search.getText().toLowerCase(Locale.ROOT).trim();current.query=search.getText();
            current.canvas.setCampaignFilters(q,type.getValue());
            if(q.isBlank()){current.lastSearch="";current.matchIndex=-1;status.setText("GameFlow structure preserved. Enter a title, fragment ID or condition to highlight events.");return;}
            var matches=current.graph.nodes.stream().filter(n->GraphCanvas.campaignMatch(n,q)).toList();
            if(matches.isEmpty()){status.setText("No matching campaign events. The full progression remains visible.");return;}
            current.matchIndex=q.equals(current.lastSearch)?(current.matchIndex+1)%matches.size():0;current.lastSearch=q;
            current.canvas.focus(matches.get(current.matchIndex).id);status.setText("Match "+(current.matchIndex+1)+" of "+matches.size()+" · Find again for the next match · progression preserved");return;
        }
        if(current.canvas!=null){
            String q=search.getText().toLowerCase(Locale.ROOT).trim();
            var matches=current.canvas.result().graph.nodes.stream().filter(n->(n.title+" "+n.text+" "+n.source).toLowerCase(Locale.ROOT).contains(q)).toList();
            if(q.isBlank()){status.setText("Enter dialogue text or a source ID, such as 6:12.");return;}
            if(matches.isEmpty()){status.setText("No matching visible source box. Check the actor filter or try another search.");return;}
            current.matchIndex=q.equals(current.lastSearch)?(current.matchIndex+1)%matches.size():0;current.lastSearch=q;
            current.canvas.focus(matches.get(current.matchIndex).id);status.setText("Match "+(current.matchIndex+1)+" of "+matches.size()+" · Find again for the next match");
        }
    }
    private void configure(View view){view.canvas.setSpeakerPalette(speakerPalette);view.canvas.onStatus=status::setText;view.canvas.onNode=(node,expand)->{
        if(expand){if(!view.expanded.add(node.id))view.expanded.remove(node.id);relayout(view);}
        else if(node.item!=null)openItem(node.item);
        else inspect(node);
    };view.canvas.onEdge=this::inspectEdge;}
    private void relayout(View view){runWork("Updating measured layout…",()->view.campaign?new LayoutEngine().campaign(view.graph,view.expanded,new TextMeasurer()):new LayoutEngine().dialogue(ActorProjection.project(view.graph,view.selectedActors),view.expanded,new TextMeasurer()),layout->{view.canvas.setResult(layout,false);show(view);});}
    private void openItem(Item item){
        if(item.conversationId()==null){history.push(current);show(new View(null,null,false,item.title(),item,detail(item),Set.of()));return;}
        Conversation conversation=data.conversations().get(item.conversationId());
        if(conversation==null){error(new IllegalArgumentException("Unresolved conversation ID: "+item.conversationId()));return;}
        buildDialogue(item.title(),item,()->new DialogueGraphBuilder().build(data,conversation));
    }
    private void buildDialogue(String name,Item item,Supplier<Graph> make){
        View previous=current;runWork("Building the complete source dialogue graph…",()->{Graph graph=make.get();return new LayoutEngine().dialogue(graph,Set.of(),new TextMeasurer());},layout->{
            var canvas=new GraphCanvas();View view=new View(canvas,layout.graph,false,name,item,null,new HashSet<>());configure(view);canvas.setSpeakerColors(speakerColors.isSelected());canvas.setResult(layout,true);history.push(previous);show(view);Platform.runLater(()->{if(!layout.boxes.isEmpty())canvas.focus(initialNode(layout));if(smokeDir!=null&&smokeStage==4)smokeDialogue();});
        });
    }
    private void show(View view){if(current!=null&&current!=view)current.query=search.getText();current=view;
        if(view.campaign){
            rootedCampaign=isRooted(view);updateTurnChoices();initializing=true;
            type.setValue(view.campaignType);turn.setValue(turn.getItems().contains(view.campaignTurn)?view.campaignTurn:"All turns");ancillary.setSelected(view.ancillaryItems);initializing=false;
        }
        search.setText(view.query);root.setRight(null);center.getChildren().setAll(view.canvas==null?view.detail:view.canvas);title.setText(view.name);title.setTooltip(new Tooltip(view.name));back.setVisible(!history.isEmpty());back.setManaged(!history.isEmpty());speakerColors.setVisible(!view.campaign&&view.canvas!=null);speakerColors.setManaged(!view.campaign&&view.canvas!=null);
        eventView.setVisible(view.campaign);eventView.setManaged(view.campaign);eventView.setText(isRooted(view)?"Event view: ROOTED":"Event view: PLAIN");
        type.setVisible(view.campaign);type.setManaged(view.campaign);turn.setVisible(view.campaign);turn.setManaged(view.campaign);ancillary.setVisible(view.campaign&&!isRooted(view));ancillary.setManaged(view.campaign&&!isRooted(view));
        search.setPromptText(view.campaign?"Find title or database name…":"Find text or source ID…");search.setVisible(view.canvas!=null);search.setManaged(view.canvas!=null);filters.getChildren().get(1).setVisible(view.canvas!=null);filters.getChildren().get(1).setManaged(view.canvas!=null);
        boolean dialogue=!view.campaign&&view.canvas!=null;
        actorFilter.setVisible(dialogue);actorFilter.setManaged(dialogue);
        if(dialogue)actorFilter.configure(view.actors,view.selectedActors,chosen->{if(current==view)filterActors(view,chosen);});
        itemDetails.setVisible(view.item!=null&&view.canvas!=null);itemDetails.setManaged(view.item!=null&&view.canvas!=null);zoomButtons.forEach(b->b.setDisable(view.canvas==null));
        notice.setText(view.campaign?(isRooted(view)?"GAMEFLOW PROGRESSION  ·  Turn → Step → Fragment. Conditions control activation; junctions show progression without assuming event causality. Gray groups share a level.  ·  Click a card to explore; › expands metadata.":"SOURCE CATALOGUE  ·  Flat items with activation predicates. Use Event view: ROOTED for GameFlow progression.  ·  Click a title to explore; › expands metadata."):view.canvas==null?"SOURCE OPTIONS  ·  Conditions and instructions are displayed exactly; this viewer does not execute them.":"SOURCE ROUTES · Each source entry appears once; conditions and effects are not evaluated. Dashed arrows mark loops. Crossing gaps are not junctions. Click an arrow to trace it or a node for metadata.");
        if(view.canvas!=null){view.canvas.setSpeakerColors(speakerColors.isSelected());view.canvas.redraw();}else status.setText(view.item.type()+"  ·  "+view.item.internalName());
    }
    private void filterActors(View view,Set<String> selected){
        Set<String> chosen=Set.copyOf(selected);
        runWork("Updating actor visibility…",()->new LayoutEngine().dialogue(ActorProjection.project(view.graph,chosen),view.expanded,new TextMeasurer()),layout->{
            view.selectedActors.clear();view.selectedActors.addAll(chosen);view.matchIndex=-1;view.lastSearch="";
            view.canvas.setResult(layout,false);root.setRight(null);
        },false);
    }
    private void inspectEdge(Graph.Edge edge){
        var layout=current.canvas.result();var from=layout.byId.get(edge.from).node();var to=layout.byId.get(edge.to).node();
        StringBuilder text=new StringBuilder("FROM: "+from.title+" ["+from.source+"]\nTO: "+to.title+" ["+to.source+"]\n"+edge.label);
        if(edge.back)text.append("\nSource back-reference / loop");
        if(edge.sourceLink!=null)text.append("\n\nEXACT JSON POINTER\n").append(edge.sourceFrom).append(" → ").append(edge.sourceTo).append("\nOrder: ").append(edge.sourceLink.order()+1).append("\nPriority: ").append(edge.sourceLink.priority()).append("\nConnector: ").append(edge.sourceLink.connector());
        if(!edge.projectionPath.isEmpty()){
            text.append("\n\nVISUAL PROJECTION THROUGH HIDDEN SPEECH\nThis arrow abbreviates the following original connectors; it is not a new JSON link.\n");
            for(var original:edge.projectionPath)text.append(original.sourceLink==null?original.from+" → "+original.to:original.sourceFrom+" → "+original.sourceTo).append("  ").append(original.label).append('\n');
        } else if(edge.sourceLink==null)text.append(isRooted(current)?"\n\nGameFlow campaign connector. Neutral junctions and group ports are layout mechanisms; only condition/choice evidence stated above establishes a specific event branch.":"\n\nInternal source-entry box sequence.");
        Label heading=new Label("Selected arrow");heading.setStyle("-fx-font-weight: bold;");Button close=new Button("×");close.setOnAction(e->root.setRight(null));
        HBox header=new HBox(16,heading,close);TextArea area=new TextArea(text.toString());area.setEditable(false);area.setWrapText(true);VBox.setVgrow(area,Priority.ALWAYS);
        inspector=new VBox(12,header,area);inspector.setPadding(new Insets(16));inspector.setPrefWidth(365);root.setRight(inspector);
    }
    private void goBack(){if(!history.isEmpty()){revision++;show(history.pop());}}
    private void inspect(Graph.Node node){
        Label heading=new Label(node.source==null?node.title:"Source "+node.source);heading.setWrapText(true);heading.setStyle("-fx-font-weight: bold; -fx-text-fill: #244332;");
        Button close=new Button("×");close.setOnAction(e->root.setRight(null));HBox head=new HBox(12,heading,close);HBox.setHgrow(heading,Priority.ALWAYS);
        String raw=node.metadata;if(node.source!=null){Entry entry=data.entry(node.source);if(entry!=null)raw+="\n\nRAW ENTRY\n"+Json.pretty(entry.raw());}
        TextArea area=new TextArea(raw);area.setEditable(false);area.setWrapText(true);area.setStyle("-fx-font-family: 'monospaced'; -fx-font-size: 11px;");VBox.setVgrow(area,Priority.ALWAYS);
        inspector=new VBox(12,head,area);inspector.setPadding(new Insets(16));inspector.setPrefWidth(365);inspector.setStyle("-fx-background-color: #fcfdf9;");root.setRight(inspector);
    }
    private javafx.scene.Node detail(Item item){
        VBox page=new VBox(20);page.setPadding(new Insets(30));page.setMaxWidth(1050);
        Label subtitle=new Label(item.type()+"  ·  "+(item.turn()==null?"Turn unspecified":"Turn "+item.turn())+"  ·  "+item.internalName());subtitle.setStyle("-fx-text-fill: #637466;");page.getChildren().add(subtitle);
        if(!item.description().isBlank())page.getChildren().add(copyBlock(item.description(),false));
        if(!item.condition().isBlank())page.getChildren().add(card("ACTIVATION",CampaignGraphBuilder.display(item.condition()),"#f7e5f2"));
        if(!item.beginInstruction().isBlank())page.getChildren().add(card("ON BEGIN",CampaignGraphBuilder.display(item.beginInstruction()),"#e2f1d8"));
        int i=0;for(Option option:item.options()){
            VBox content=new VBox(12);content.setPadding(new Insets(18));content.setStyle("-fx-background-color: #fffefa; -fx-background-radius: 10; -fx-border-color: #cbd5ca; -fx-border-radius: 10;");
            Label name=new Label((++i)+". "+option.title());name.setWrapText(true);name.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2b4935;");content.getChildren().add(name);
            if(!option.condition().isBlank())content.getChildren().add(card("CONDITION",CampaignGraphBuilder.display(option.condition()),"#f7e5f2"));
            if(!option.instruction().isBlank())content.getChildren().add(card("SOURCE INSTRUCTIONS / EFFECTS",CampaignGraphBuilder.display(option.instruction()),"#e2f1d8"));
            page.getChildren().add(content);
        }
        if(!item.endInstruction().isBlank())page.getChildren().add(card("ON END",CampaignGraphBuilder.display(item.endInstruction()),"#e2f1d8"));
        TitledPane source=new TitledPane("Complete source metadata",copyBlock(Json.pretty(item.raw()),true));source.setExpanded(false);page.getChildren().add(source);
        ScrollPane scroll=new ScrollPane(page);scroll.setFitToWidth(true);scroll.setStyle("-fx-background: #f3f1ec; -fx-background-color: #f3f1ec;");return scroll;
    }
    private VBox card(String title,String body,String color){Label h=new Label(title);h.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");VBox box=new VBox(9,h,copyBlock(body,true));box.setPadding(new Insets(14));box.setStyle("-fx-background-color: "+color+"; -fx-background-radius: 8;");return box;}
    private TextArea copyBlock(String value, boolean mono) {
        TextArea text = new TextArea(value);
        text.setEditable(false);
        text.setWrapText(true);
        text.setFocusTraversable(false);

        int rows = Math.min(26, Math.max(4, value.split("\\R").length + value.length() / 110));
        text.setPrefRowCount(rows);

        text.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #1f2a21;" +
                        "-fx-highlight-fill: #cfe5d6;" +
                        "-fx-highlight-text-fill: #1f2a21;" +
                        "-fx-control-inner-background: #fffdfa;" +
                        "-fx-background-color: #fffdfa;" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #d6ddd2;" +
                        "-fx-border-radius: 6;" +
                        (mono ? "-fx-font-family: 'Consolas', 'Courier New', monospace;" : "")
        );

        return text;
    }


    private void diagnostics(){
        if(data==null)return;String message="DATA DISCOVERY\n"+String.join("\n\n",data.diagnostics());if(current!=null&&current.canvas!=null)message+="\n\nCURRENT GRAPH\n"+String.join("\n\n",current.canvas.result().graph.diagnostics);
        Dialog<Void> dialog=new Dialog<>();dialog.initOwner(stage);dialog.setTitle("Source coverage and unresolved relationships");TextArea area=new TextArea(message);area.setEditable(false);area.setWrapText(true);area.setPrefSize(850,570);dialog.getDialogPane().setContent(area);dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);dialog.show();
    }
    private <T> void runWork(String label,Callable<T> job,Consumer<T> success){runWork(label,job,success,true);}
    private <T> void runWork(String label,Callable<T> job,Consumer<T> success,boolean freezeToolbar){
        if(activeTask!=null)activeTask.cancel(true);
        long token=++revision;top.setDisable(freezeToolbar);ProgressIndicator progress=new ProgressIndicator();progress.setMaxSize(30,30);Label message=new Label(label);VBox busy=new VBox(16,progress,message);busy.setAlignment(Pos.CENTER);busy.setStyle("-fx-background-color: rgba(243,241,236,0.96);");center.getChildren().add(busy);status.setText(label);
        Task<T> task=new Task<>(){@Override protected T call() throws Exception{return job.call();}};
        task.setOnSucceeded(e->{center.getChildren().remove(busy);if(token==revision){top.setDisable(false);activeTask=null;success.accept(task.getValue());}});
        task.setOnFailed(e->{center.getChildren().remove(busy);if(token==revision){top.setDisable(false);activeTask=null;if(current!=null)show(current);error(task.getException());}});
        task.setOnCancelled(e->center.getChildren().remove(busy));
        Button cancel=new Button("Cancel");cancel.setOnAction(e->{revision++;task.cancel(true);activeTask=null;top.setDisable(false);if(current!=null)show(current);else loadCancelled();});busy.getChildren().add(cancel);activeTask=task;worker.submit(task);
    }
    private void error(Throwable failure){failure.printStackTrace();if(data==null)loadCancelled();status.setText("Could not complete operation: "+failure.getMessage());Alert alert=new Alert(Alert.AlertType.ERROR);alert.initOwner(stage);alert.setTitle("Sordland viewer");alert.setHeaderText("The source files or graph could not be loaded");alert.setContentText(failure.getMessage()+"\n\nChoose structurally valid entity and Sordland conversation JSON files.");alert.show();if(smokeDir!=null){System.err.println("SMOKE FAILED");Platform.exit();}}
    private void snapshot(String name) throws Exception {
        root.applyCss();root.layout();WritableImage image=root.snapshot(null,null);int w=(int)image.getWidth(),h=(int)image.getHeight();var bitmap=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);int[] pixels=new int[w*h];image.getPixelReader().getPixels(0,0,w,h,javafx.scene.image.PixelFormat.getIntArgbInstance(),pixels,0,w);bitmap.setRGB(0,0,w,h,pixels,0,w);Path dir=Path.of(smokeDir);Files.createDirectories(dir);javax.imageio.ImageIO.write(bitmap,"png",dir.resolve(name+".png").toFile());
    }
    private static void smokeCheck(boolean condition,String message){if(!condition)throw new IllegalStateException("SMOKE: "+message);}
    private void smokeDetails(Graph.Node node,String filename) throws Exception {
        View previous=current;current.canvas.onNode.accept(node,false);
        smokeCheck(current.canvas==null&&current.item==node.item&&current.detail!=null,"event click opens existing details: "+filename);
        snapshot(filename);goBack();smokeCheck(current==previous,"Back restores the same campaign view");
    }
    private void smokeCampaign(){
        try{
            if(smokeStage==0){
                smokeCheck(isRooted(current)&&eventView.getText().equals("Event view: ROOTED"),"ROOTED default");
                smokeCheck(current.graph.nodes.getFirst().title.equals("START"),"continuous START root");
                smokeCheck(!ancillary.isVisible(),"ancillary hidden in ROOTED");snapshot("campaign-rooted");
                smokeStage=1;eventView.fire();return;
            }
            if(smokeStage==1){
                smokeCheck(!isRooted(current)&&eventView.getText().equals("Event view: PLAIN"),"toggle to PLAIN");
                smokeCheck(ancillary.isVisible(),"ancillary retained in PLAIN");snapshot("campaign-plain");
                smokeDetails(current.graph.nodes.stream().filter(n->n.item!=null&&n.item.type().equals("Bill")).findFirst().orElseThrow(),"bill-plain");
                smokeStage=2;eventView.fire();return;
            }
            if(smokeStage==2){
                smokeCheck(isRooted(current),"toggle back to ROOTED");smokeStage=3;turn.setValue("Turn 3");return;
            }
            if(smokeStage==3){
                smokeCheck(isRooted(current)&&current.graph.nodes.getFirst().title.startsWith("TURN START"),"isolated turn has TURN START");
                Graph graph=current.graph;var layout=current.canvas.result();
                search.setText("Turn03_Decision_Extraction");search();
                smokeCheck(current.graph==graph&&current.canvas.result()==layout,"search keeps graph and layout");
                smokeCheck(current.canvas.selected()!=null&&layout.byId.get(current.canvas.selected()).node().item.internalName().equals("Turn03_Decision_Extraction"),"search focuses Extraction");
                type.setValue("Decision");smokeCheck(current.graph==graph&&current.canvas.result()==layout,"type filtering preserves graph and layout");
                current.canvas.zoom(.8);snapshot("gasom-branches");
                type.setValue("All types");search.clear();search();
                smokeDetails(graph.nodes.stream().filter(n->n.item!=null&&n.item.internalName().equals("Turn03_Decision_Extraction")).findFirst().orElseThrow(),"decision-rooted");
                smokeDetails(graph.nodes.stream().filter(n->n.item!=null&&n.item.type().equals("Bill")).findFirst().orElseThrow(),"bill-rooted");
                smokeReturnView=current;smokeStage=4;
                var conversation=graph.nodes.stream().filter(n->n.item!=null&&n.item.conversationId()!=null).findFirst().orElseThrow();
                current.canvas.onNode.accept(conversation,false);return;
            }
        }catch(Exception e){error(e);}
    }
    private void smokeDialogue(){
        try{
            smokeCheck(!current.campaign&&current.canvas!=null&&current.graph.campaign==null,"conversation opens existing dialogue graph");
            smokeCheck(!eventView.isVisible()&&actorFilter.isVisible(),"campaign toggle does not affect dialogue controls");
            Graph graph=current.graph;snapshot("dialogue");current.canvas.setSpeakerColors(true);snapshot("dialogue-colors");
            smokeCheck(current.graph==graph,"speaker colors preserve dialogue graph");goBack();
            smokeCheck(current==smokeReturnView&&isRooted(current),"dialogue Back preserves rooted campaign");
            smokeExitCode=0;System.out.println("SMOKE PASSED: ROOTED default; both toggles; turn/search/type filters; Decision/Bill and dialogue clicks; retained Back navigation; snapshots at "+smokeDir);
        }catch(Exception e){e.printStackTrace();}finally{Platform.exit();}
    }
    @Override public void stop(){worker.shutdownNow();if(smokeMode)System.exit(smokeExitCode);}
    public static void main(String[] args){
        smokeMode=Arrays.stream(args).anyMatch(a->a.startsWith("--smoke="));
        if(smokeMode){Thread watchdog=new Thread(()->{try{Thread.sleep(60_000);System.err.println("SMOKE FAILED: no completed JavaFX snapshot within 60 seconds. A graphical display is required.");Runtime.getRuntime().halt(2);}catch(InterruptedException ignored){}},"smoke-timeout");watchdog.setDaemon(true);watchdog.start();}
        launch(args);
    }
}
