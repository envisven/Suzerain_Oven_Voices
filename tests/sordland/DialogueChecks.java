package sordland;

import sordland.data.Domain.*;
import sordland.graph.*;
import sordland.layout.LayoutEngine;
import java.util.*;
import static sordland.TestSupport.*;

final class DialogueChecks {
    private DialogueChecks() {}
    static void run() {
        var builder=new DialogueGraphBuilder();
        Dataset flavor=diamond("","","");
        var shared=builder.build(flavor,flavor.conversations().get(100));
        equal(1L,spokenOccurrences(shared,3),"Pure flavour branches share their exact downstream narrator once");
        check(shared.nodes.stream().anyMatch(n->n.kind==Graph.Kind.CHOICE&&n.title.contains("1")),"Player response is an ordered visible box");
        check(shared.nodes.stream().anyMatch(n->n.kind==Graph.Kind.CHOICE&&n.title.contains("2")),"Second player response is a separate visible box");
        check(shared.nodes.stream().anyMatch(n->n.kind==Graph.Kind.NARRATOR&&n.title.equals("NARRATOR")),"Narration is identified as NARRATOR");
        var start=shared.nodes.stream().filter(n->new EntryKey(100,0).equals(n.source)).findFirst().orElseThrow();
        var byId=index(shared);List<Integer> children=shared.edges.stream().filter(e->e.from.equals(start.id)).map(e->byId.get(e.to).source.dialogueId()).toList();
        equal(List.of(1,2),children,"Outgoing player choices follow exact source link order");
        var positioned=new LayoutEngine().dialogue(shared,Set.of(),LayoutChecks.MEASURE);
        LayoutChecks.assertGeometry(positioned,"Exact shared flavour graph");

        for(var altered:List.of(diamond("BaseGame.Flag = true;","",""),diamond("","BaseGame.Ready == true",""),diamond("MaybeSetPolicy(42);","",""))) {
            var graph=builder.build(altered,altered.conversations().get(100));
            equal(2L,spokenOccurrences(graph,3),"Effects, path predicates and unknown commands keep downstream contexts distinct");
            assertReferences(graph);
        }
        var changed=builder.build(diamond("BaseGame.Flag = true;","",""),diamond("BaseGame.Flag = true;","","").conversations().get(100));
        check(changed.nodes.stream().anyMatch(n->n.kind==Graph.Kind.EFFECT&&n.text.equals("Flag = true")),"Gameplay mutation shown as dedicated effect box");
        var cosmetic=diamond("PlaySceneMusic(\"song\"); Continue();","","");
        equal(1L,spokenOccurrences(builder.build(cosmetic,cosmetic.conversations().get(100)),3),"Confirmed presentation-only commands permit flavor reconvergence");

        Dataset stable=loop(false);var stableGraph=builder.build(stable,stable.conversations().get(100));
        check(stableGraph.edges.stream().anyMatch(e->e.back),"Unchanged-context source loop becomes back-reference");
        check(stableGraph.nodes.size()<10,"Unchanged-context loop never recursively expands forever");
        Dataset changing=loop(true);var changingGraph=builder.build(changing,changing.conversations().get(100));
        var portal=changingGraph.nodes.stream().filter(n->n.continuation!=null).findFirst().orElseThrow();
        check(portal.continuation.context.depth>0,"Changed-state loop retains accumulated history in continuation");
        var resumed=builder.buildContinuation(changing,portal.continuation);
        check(resumed.nodes.stream().filter(n->n.continuation!=null).anyMatch(n->n.continuation.context.depth>portal.continuation.context.depth),"Opening changed-state loop preserves and extends semantic history");
        assertReferences(stableGraph);assertReferences(changingGraph);assertReferences(resumed);

        
        
        var caller=dataset(List.of(entry(0,10,"START","","","","",1),entry(1,10,"Narrator","Call","","",""),entry(2,10,"Narrator","Returned","","","")));
        var callerEntries=new LinkedHashMap<>(caller.conversations().get(100).entries());
        var call=callerEntries.get(1);callerEntries.put(1,new Entry(call.key(),call.actorId(),call.speaker(),call.title(),call.text(),"","","","",List.of(new Link(new EntryKey(200,0),0,"Normal",false)),Map.of()));
        var returned=new Entry(new EntryKey(200,0),10,"Narrator","Narrator","Callee","","","","",List.of(new Link(new EntryKey(100,2),0,"Normal",false)),Map.of());
        var cross=new Dataset(List.of(),Map.of(100,new Conversation(100,"Sordland/Caller",callerEntries),200,new Conversation(200,"Sordland/Callee",Map.of(0,returned))),List.of(),List.of());
        var calledGraph=builder.build(cross,cross.conversations().get(100));
        equal(1L,spokenOccurrences(calledGraph,2),"Cross-conversation return is reachable once in caller context");
        check(calledGraph.diagnostics.stream().noneMatch(d->d.contains("outside the first root")),"Reachability follows exact cross-conversation call/return links");
        var guardedHistory=builder.build(diamond("BaseGame.Flag = true;","",""),diamond("BaseGame.Flag = true;","","").conversations().get(100));
        check(guardedHistory.nodes.stream().anyMatch(n->n.source!=null&&n.source.dialogueId()==3&&n.context!=null&&n.context.history().stream().anyMatch(h->h.contains("BaseGame.Flag = true"))),"Downstream inspector can access exact prior semantic history");
        String largeScript="BaseGame.Flag = true;".repeat(30);
        var large=dataset(List.of(entry(0,10,"START","","",largeScript,"",1),entry(1,10,"Narrator","After large source instruction","","","")));
        var atomic=new DialogueGraphBuilder(8).build(large,large.conversations().get(100));
        check(atomic.nodes.stream().anyMatch(n->n.kind==Graph.Kind.EFFECT),"Oversized source occurrence advances instead of repeating the same continuation forever");
        check(atomic.diagnostics.stream().anyMatch(d->d.contains("atomically")),"Atomic source entry overflow is explicitly diagnosed");

        Dataset linear=chain(73);DialogueGraphBuilder bounded=new DialogueGraphBuilder(12);
        var pending=new ArrayDeque<Graph>();pending.add(bounded.build(linear,linear.conversations().get(100)));
        Set<EntryKey> seen=new HashSet<>();int chunks=0;
        while(!pending.isEmpty()) {
            var graph=pending.removeFirst();check(++chunks<30,"Finite source chain finishes across bounded chunks");
            check(graph.nodes.size()<=12,"Chunk obeys visual node budget");assertReferences(graph);
            for(var node:graph.nodes) {
                if(node.continuation!=null)pending.addLast(bounded.buildContinuation(linear,node.continuation));
                else if(node.source!=null)seen.add(node.source);
            }
        }
        equal(73,seen.size(),"Continuation boundaries retain access to every original source entry");
    }
    static void realData(Dataset data) {
        DialogueGraphBuilder builder=new DialogueGraphBuilder();LayoutEngine layout=new LayoutEngine();
        int count=0,boxes=0,portals=0;
        for(var conversation:data.conversations().values()) {
            var graph=builder.build(data,conversation);assertReferences(graph);
            check(!graph.nodes.isEmpty(),"Every supplied conversation produces an inspectable graph");
            for(var node:graph.nodes)if(node.source!=null)check(data.entry(node.source)!=null,"Real graph occurrence resolves exact source identity");
            var positioned=layout.dialogue(graph,Set.of(),new sordland.ui.TextMeasurer());
            LayoutChecks.assertGeometry(positioned,"Conversation "+conversation.id());
            LayoutChecks.assertConnectors(positioned);
            var choiceChildren=new LinkedHashMap<String,List<String>>();
            for(var edge:graph.edges)if(!edge.back){
                var target=positioned.byId.get(edge.to).node();
                if(target.source!=null){var sourceEntry=data.entry(target.source);if(sourceEntry.isPlayer()&&(!sourceEntry.text().isBlank()||!sourceEntry.menuText().isBlank()))choiceChildren.computeIfAbsent(edge.from,k->new ArrayList<>()).add(edge.to);}
            }
            for(var children:choiceChildren.values())for(int i=1;i<children.size();i++)check(positioned.byId.get(children.get(i-1)).cx()<positioned.byId.get(children.get(i)).cx(),"Source player choices remain left-to-right in conversation "+conversation.id()+": "+children);
            boxes+=graph.nodes.size();portals+=graph.nodes.stream().filter(n->n.continuation!=null).count();count++;
        }
        System.out.println("Real dialogue coverage: "+count+" conversation first chunks; "+boxes+" visual boxes; "+portals+" explicit continuations.");
    }
    private static long spokenOccurrences(Graph graph,int id) {return graph.nodes.stream().filter(n->new EntryKey(100,id).equals(n.source)&&n.kind==Graph.Kind.NARRATOR).count();}
    private static Map<String,Graph.Node> index(Graph graph){var result=new LinkedHashMap<String,Graph.Node>();graph.nodes.forEach(n->result.put(n.id,n));return result;}
    private static void assertReferences(Graph graph) {
        var ids=index(graph);equal(graph.nodes.size(),ids.size(),"Graph visual occurrence IDs are unique");
        for(var edge:graph.edges)check(ids.containsKey(edge.from)&&ids.containsKey(edge.to),"Every visual connector endpoint exists");
    }
    private static Dataset diamond(String script,String condition,String sequence) {
        return dataset(List.of(entry(0,10,"START","","","","",1,2),entry(1,5,"Player","Left response",condition,script,sequence,3),entry(2,5,"Player","Right response","","","",3),entry(3,10,"Narrator","Shared exact source sentence","","","",4),entry(4,10,"End","","","End();","")));
    }
    private static Dataset loop(boolean changes){return dataset(List.of(entry(0,10,"START","","","","",1),entry(1,10,"Narrator","Visit again","","","",2),entry(2,10,"Narrator","Repeat","",changes?"BaseGame.Count += 1;":"","",1)));}
    private static Dataset chain(int count) {var entries=new ArrayList<Entry>();for(int i=0;i<count;i++)entries.add(entry(i,10,i==0?"START":"Narrator",i==0?"":"Line "+i,"","","",i+1<count?new int[]{i+1}:new int[]{}));return dataset(entries);}
    private static Dataset dataset(List<Entry> entries) {var map=new LinkedHashMap<Integer,Entry>();entries.forEach(e->map.put(e.key().dialogueId(),e));var c=new Conversation(100,"Sordland/Turn01/Synthetic",map);return new Dataset(List.of(),Map.of(100,c),List.of(),List.of());}
    private static Entry entry(int id,int actor,String speaker,String text,String condition,String script,String sequence,int... destinations) {
        List<Link> links=new ArrayList<>();for(int i=0;i<destinations.length;i++)links.add(new Link(new EntryKey(100,destinations[i]),i,"Normal",false));
        return new Entry(new EntryKey(100,id),actor,speaker,speaker,text,"",condition,script,sequence,links,Map.of());
    }
}
