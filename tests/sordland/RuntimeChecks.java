package sordland;

import sordland.data.*;
import sordland.data.Domain.*;
import sordland.data.runtime.*;
import sordland.graph.*;
import sordland.graph.Graph.*;
import sordland.layout.*;
import sordland.layout.LayoutEngine.*;
import java.nio.file.*;
import java.util.*;
import static sordland.TestSupport.*;

                                                                                                       
final class RuntimeChecks {
    static void run(Path path,Dataset data)throws Exception {
        RuntimeDatabase db=data.runtime();
        Map<String,Integer> counts=Map.ofEntries(Map.entry("pageddecisionpanelsdata",8),Map.entry("carouselchoicepagedata",17),Map.entry("carouselchoiceoptiondata",55),Map.entry("multiplechoicepagedata",24),Map.entry("multiplechoiceoptiondata",64),Map.entry("policiesdata",164),Map.entry("situationsdata",228),Map.entry("reportsdata",862),Map.entry("decreesdata",19),Map.entry("journalentriesdata",561),Map.entry("tokenstatuseffectsdata",106));
        for(var expected:counts.entrySet())equal(expected.getValue(),db.collection(expected.getKey()).size(),"Sordland runtime count "+expected.getKey());
        equal(162,Json.list(db.catalog.get("members")).size(),"All catalog members preserved");
        for(var collection:db.collections.entrySet())for(var entity:collection.getValue()){
            check(entity.storyPacks().contains("StoryPack_Main"),"Only exact main story pack enters runtime index");
            check(!entity.graphEligible()||!entity.path().startsWith("Rizia/"),"Conflicting Rizia paths cannot enter gameplay graphs");
            check(db.byName.get(entity.name()).contains(entity),"Exact name indexed");
            check(entity.id().isBlank()||db.byId.get(entity.id()).contains(entity),"Exact Id indexed");
            var source=Json.object(db.sourceFiles.get(entity.file()));
            equal(RuntimeDatabase.values(source.get("data")).get(entity.index()),entity.raw(),"Complete raw object and original index retained");
            var preferred=Json.list(db.catalog.get("members")).stream().map(Json::object).filter(m->entity.collection().equals(Json.string(m,"logicalKey"))&&Boolean.TRUE.equals(m.get("preferredForCoverage"))).toList();
            equal(1,preferred.size(),"One preferred member");equal(Json.string(preferred.getFirst(),"outputFile"),entity.file(),"Catalog outputFile selected");
        }
        var panel=db.resolve("Panel_Budget","pageddecisionpanelsdata");check(panel!=null,"Exact Budget panel resolves");
        equal("BaseGame.GovernmentBudget",panel.value("PanelCounterVariable"),"Counter provenance");
        var entry=data.entry(new EntryKey(26,209));check(entry!=null,"26:209 exists");
        check(Semantics.analyze(entry.script(),entry.sequence()).commands().stream().anyMatch(c->c.kind()==Semantics.CommandKind.PANEL&&"Panel_Budget".equals(PanelGraphBuilder.panelName(c.raw()))),"Structured panel command");
        Map<String,int[]> budgets=Map.of("Education",new int[]{2,0,-1,-3,0,2},"Healthcare",new int[]{1,0,-2,-2,0,3},"Security",new int[]{1,0,-1,-3,0,2},"Military",new int[]{-1,0,-2,-4,0,3});
        for(var area:budgets.entrySet())for(int i=0;i<3;i++){
            String choice=List.of("Increase","Maintain","Decrease").get(i),name="Option_Budget_"+area.getKey()+"_"+choice;
            var option=db.resolve(name,"carouselchoiceoptiondata");check(option!=null,"Exact option "+name);
            String flag="BaseGame.Turn03_EnT_"+(area.getKey().equals("Security")?"LawEnforcement":area.getKey())+"_Budget_"+choice+" = true";
            check(option.value("Instruction").contains(flag),"Exact boolean flag "+name);
            int economy=area.getValue()[i];String instruction=option.value("Instruction");
            if(economy==0)check(!instruction.contains("BaseGame.Economy"),"No fake economy on Maintain");
            else check(instruction.contains("BaseGame.Economy "+(economy>0?"+= ":"-= ")+Math.abs(economy)),"Exact Economy delta "+name);
            equal(area.getValue()[i+3],((Number)option.properties().get("PanelCounterIncrement")).intValue(),"Counter delta "+name);
        }
        Graph graph=new DialogueGraphBuilder().build(data,data.conversations().get(26));
        PanelGroup group=graph.panels.stream().filter(p->graph.nodes.stream().anyMatch(n->n.id.equals(p.entryId())&&n.item!=null&&n.item.internalName().equals("Panel_Budget"))).findFirst().orElseThrow();
        equal(4,group.categories().size(),"Four funding sets");equal(18,group.members().size(),"One entry, four headers, twelve bundles, one completion");
        var ids=new HashMap<String,Node>();graph.nodes.forEach(n->ids.put(n.id,n));
        for(var category:group.categories()){
            equal(3,category.branches().size(),"Three choices per funding set");
            for(int i=0;i<3;i++){
                var branch=category.branches().get(i);var node=ids.get(branch.effectId());
                check(node.title.startsWith(List.of("INCREASE","MAINTAIN","DECREASE").get(i)),"Semantic option ordering");
                check(node.kind==Kind.EFFECT,"One existing-style effect card per option");
                check(node.metadata.contains("PanelCounterVariable (panel source): BaseGame.GovernmentBudget"),"Panel source retained");
                check(node.metadata.contains("PanelCounterIncrement (option source):"),"Counter source retained");
                check(node.metadata.contains("Instruction")&&node.metadata.contains("Condition"),"Raw instruction and condition retained");
                check(graph.edges.stream().noneMatch(e->e.from.equals(node.id)),"Choices do not cause dialogue continuation");
            }
        }
        var continuation=graph.edges.stream().filter(e->new EntryKey(26,209).equals(e.sourceFrom)).toList();
        equal(1,continuation.size(),"One exact Budget continuation");equal(new EntryKey(27,1),continuation.getFirst().sourceTo,"Exact continuation destination");equal(group.completionId(),continuation.getFirst().from,"Continuation leaves panel completion");
        for(var area:budgets.entrySet())for(int i=0;i<3;i++){
            String choice=List.of("Increase","Maintain","Decrease").get(i),name="Option_Budget_"+area.getKey()+"_"+choice;
            Node card=graph.nodes.stream().filter(n->n.kind==Kind.EFFECT&&n.item!=null&&n.item.internalName().equals(name)).findFirst().orElseThrow();
            String flag=(area.getKey().equals("Security")?"LawEnforcement":area.getKey())+"Budget"+choice+" = true";
            check(card.text.contains(flag),"Rendered exact choice flag "+name);
            int eco=area.getValue()[i],counter=area.getValue()[i+3];
            if(eco==0)check(!card.text.contains("Economy"),"Rendered Maintain has no Economy change");
            else check(card.text.contains("Economy "+(eco>0?"+":"")+eco),"Rendered Economy delta "+name);
            if(counter==0)check(!card.text.contains("GovernmentBudget"),"Zero counter stays in source without visual noise");
            else check(card.text.contains("GovernmentBudget "+(counter>0?"+":"")+counter),"Rendered GovernmentBudget delta "+name);
        }
        var layout=new LayoutEngine().dialogue(graph,Set.of(),RuntimeChecks::measure);geometry(layout,group);
        var projected=ActorProjection.project(graph,Set.of());equal(graph.panels,projected.panels,"Actor filter retains panel mechanics");geometry(new LayoutEngine().dialogue(projected,Set.of(),RuntimeChecks::measure),group);
        var expanded=new LayoutEngine().dialogue(graph,Set.of(group.categories().getFirst().branches().getFirst().effectId()),RuntimeChecks::measure);geometry(expanded,group);
                                                                                                       
        int graphs=0;
        for(Conversation c:data.conversations().values()){
            Graph g=new DialogueGraphBuilder().build(data,c);graphs++;var map=new HashMap<String,Node>();g.nodes.forEach(n->check(map.put(n.id,n)==null,"Unique enriched node id"));
            var seen=new HashSet<EntryKey>();g.nodes.stream().map(n->n.source).filter(Objects::nonNull).forEach(seen::add);
            for(EntryKey key:seen){Entry e=data.entry(key);if(e==null)continue;var links=g.edges.stream().filter(edge->key.equals(edge.sourceFrom)).toList();equal(e.links().size(),links.size(),"Runtime exact link cardinality "+key);for(Link l:e.links())equal(1L,links.stream().filter(edge->edge.sourceLink==l).count(),"Exact Link identity retained once");}
            for(Edge edge:g.edges)check(map.containsKey(edge.from)&&map.containsKey(edge.to),"Runtime edge endpoints exist");
        }
        equal(286,graphs,"All runtime-enriched Sordland graphs audited");
        var plain=new CampaignGraphBuilder().build(data,"",null,null,true);
        for(String type:RuntimeDatabase.TYPES.values()){check(TypeProjection.types(plain).contains(type),"Runtime PLAIN type "+type);check(!TypeProjection.defaults(TypeProjection.types(plain)).contains(type),"Runtime default off "+type);check(!new TypeSelection().selectedFor(List.of(type)).contains(type),"Remembered selection default off");}
        var rooted=new RootedCampaignGraphBuilder().build(data,null);check(!rooted.campaign.runtime().isEmpty(),"Exact runtime evidence enters ROOTED");
        System.out.println("Runtime ROOTED types: "+rooted.nodes.stream().filter(n->n.item!=null&&n.item.id().startsWith("runtime:")).map(n->n.type).distinct().sorted().toList());
        var defaultGraph=TypeProjection.project(rooted,TypeProjection.defaults(TypeProjection.types(rooted)));check(defaultGraph.campaign.runtime().isEmpty(),"Runtime annotations hidden by default");
        new LayoutEngine().campaign(defaultGraph,Set.of(),RuntimeChecks::measure);
        new LayoutEngine().campaign(rooted,Set.of(),RuntimeChecks::measure);
        check(RuntimeGraphBuilder.evidence(db,"if BaseGame.X then BaseGame.Policy_Diplomacy_Agnolia_Alliance = true; end").isEmpty(),"No speculative evidence inside unsupported control flow");
        check(RuntimeGraphBuilder.evidence(db,"BaseGame.Unrelated = true").isEmpty(),"No variable co-occurrence links");
        check(!RuntimeGraphBuilder.evidence(db,"BaseGame.Policy_Diplomacy_Agnolia_Alliance = true").isEmpty(),"Exact enabled variable resolves");
        check(!RuntimeGraphBuilder.evidence(db,"AddTokenStatus(\"Sordland_City_Lachaven\", \"Progress_Highway\")").isEmpty(),"Exact token status argument resolves");
        check(RuntimeGraphBuilder.evidence(db,"AddReport(\"not_a_report\")").isEmpty(),"No fuzzy report match");
        check(plain.nodes.stream().filter(n->n.item!=null&&n.item.id().startsWith("runtime:")).noneMatch(n->n.item.path().startsWith("Rizia/")),"No conflicting Rizia record rendered in PLAIN");
        check(rooted.nodes.stream().filter(n->n.item!=null&&n.item.id().startsWith("runtime:")).noneMatch(n->n.item.path().startsWith("Rizia/")),"No Rizia record rendered in ROOTED");
        brokenFixtures(db,panel);
        System.out.println("Runtime counts: "+counts+"; 286 enriched dialogue graphs, exact Budget effects/continuation and panel geometry PASS.");
    }
    static Size measure(Node n,boolean expanded){return n.kind==Kind.JUNCTION?new Size(2,2,List.of(),List.of(),List.of()):new Size(expanded?480:300,expanded?900:90+18*n.text.lines().count(),List.of(n.title),List.of(n.text),List.of());}
    private static void geometry(Result layout,PanelGroup panel){
        Group group=layout.groups.stream().filter(g->g.id().equals(panel.id())).findFirst().orElseThrow();
        Double y=null;
        for(var c:panel.categories()){
            Box header=layout.byId.get(c.headerId());if(y==null)y=header.y();equal(y,header.y(),"Header alignment");
            for(var b:c.branches()){Box effect=layout.byId.get(b.effectId());check(effect.y()>header.bottom(),"Choices below ministry");}
        }
        for(String id:panel.members()){Box b=layout.byId.get(id);check(b.x()>=group.x()&&b.x()+b.w()<=group.x()+group.width()+0.01&&b.y()>=group.y()&&b.bottom()<=group.bottom()+0.01,"Outer group contains member "+id);check(b.node().kind==Kind.JUNCTION||layout.hit(b.cx(),b.y()+2)==b,"Member hit testing");}
        for(int i=0;i<layout.boxes.size();i++)for(int j=i+1;j<layout.boxes.size();j++){Box a=layout.boxes.get(i),b=layout.boxes.get(j);check(!(a.x()<b.x()+b.w()&&a.x()+a.w()>b.x()&&a.y()<b.bottom()&&a.bottom()>b.y()),"No overlapping enriched boxes");}
        for(Line line:layout.lines)for(Box box:layout.boxes){
            if(box.node().id.equals(line.edge().from)||box.node().id.equals(line.edge().to))continue;
            for(int i=1;i<line.points().size();i++){Point a=line.points().get(i-1),b=line.points().get(i);boolean through=a.x()==b.x()?a.x()>box.x()+.01&&a.x()<box.x()+box.w()-.01&&Math.max(a.y(),b.y())>box.y()+.01&&Math.min(a.y(),b.y())<box.bottom()-.01:a.y()==b.y()&&a.y()>box.y()+.01&&a.y()<box.bottom()-.01&&Math.max(a.x(),b.x())>box.x()+.01&&Math.min(a.x(),b.x())<box.x()+box.w()-.01;check(!through,"No routed segment crosses unrelated card: "+line.edge().from+" -> "+line.edge().to+" across "+box.node().id);}
        }
        for(Line line:layout.lines)if(line.edge().to.equals(panel.entryId())){
            Point end=line.points().getLast();equal(group.y(),end.y(),"Incoming edge reaches group top");equal(group.cx(),end.x(),"Incoming edge reaches group middle");
        }
        for(var category:panel.categories())for(Line line:layout.lines)if(line.edge().from.equals(category.headerId())){
            Point start=line.points().getFirst();Box header=layout.byId.get(category.headerId());equal(header.bottom(),start.y(),"Branch leaves correct header bottom");equal(header.cx(),start.x(),"Branch leaves correct header center");
        }
        equal(group.y(),layout.byId.get(panel.entryId()).y(),"Group input at top");equal(group.cx(),layout.byId.get(panel.entryId()).cx(),"Input top middle");
    }
    private static void brokenFixtures(RuntimeDatabase db,RuntimeDatabase.Entity panel)throws Exception {
        var nodes=new ArrayList<Node>();var edges=new ArrayList<Edge>();var groups=new ArrayList<PanelGroup>();var notes=new ArrayList<String>();
        PanelGraphBuilder.append(RuntimeDatabase.empty(),"ShowPagedDecisionsPanel(\"Panel_Budget\")","missing",new EntryKey(26,209),nodes,edges,groups,notes);
        check(!notes.isEmpty()&&nodes.getFirst().kind==Kind.NOTICE,"Missing runtime explicit diagnostic");
        var broken=new RuntimeDatabase(Map.of("pageddecisionpanelsdata",List.of(panel)),db.catalog,db.sourceFiles,List.of());nodes.clear();notes.clear();
        PanelGraphBuilder.append(broken,"ShowPagedDecisionsPanel(\"Panel_Budget\")","broken",null,nodes,edges,groups,notes);equal(4L,nodes.stream().filter(n->n.kind==Kind.NOTICE).count(),"All missing pages retained as diagnostics");
        var noOptions=new RuntimeDatabase(Map.of("pageddecisionpanelsdata",List.of(panel),"carouselchoicepagedata",db.collection("carouselchoicepagedata")),db.catalog,db.sourceFiles,List.of());nodes.clear();notes.clear();edges.clear();groups.clear();
        PanelGraphBuilder.append(noOptions,"ShowPagedDecisionsPanel(\"Panel_Budget\")","brokenOptions",null,nodes,edges,groups,notes);equal(12L,nodes.stream().filter(n->n.kind==Kind.NOTICE).count(),"All missing options retained");
        Path absent=Files.createTempDirectory("sordland-runtime-test-");try{var empty=RuntimeDatabaseLoader.load(absent);check(empty.collections.isEmpty()&&!empty.diagnostics.isEmpty(),"Runtime absence graceful");}finally{Files.delete(absent);}
                                                                                     
        for(var p:db.collection("pageddecisionpanelsdata")){nodes.clear();edges.clear();groups.clear();notes.clear();PanelGraphBuilder.append(db,"ShowPagedDecisionsPanel(\""+p.name()+"\")","panel",null,nodes,edges,groups,notes);check(!groups.isEmpty(),"Generic panel resolves "+p.name());for(Node n:nodes)if(n.kind==Kind.CONDITION){equal(1L,edges.stream().filter(e->e.from.equals(n.id)&&e.label.equals("TRUE")).count(),"Condition TRUE only");check(edges.stream().noneMatch(e->e.from.equals(n.id)&&e.label.equals("FALSE")),"No invented false destination");}}
    }
}
