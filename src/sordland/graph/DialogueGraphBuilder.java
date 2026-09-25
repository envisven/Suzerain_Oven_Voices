package sordland.graph;

import sordland.data.Domain.*;
import sordland.graph.Graph.*;
import sordland.graph.Semantics.CommandKind;
import java.util.*;



public final class DialogueGraphBuilder {
    public static final int DEFAULT_NODE_LIMIT = 4000;
    private final int nodeLimit;

    public DialogueGraphBuilder() { this(DEFAULT_NODE_LIMIT); }
    public DialogueGraphBuilder(int nodeLimit) {
        if (nodeLimit < 8) throw new IllegalArgumentException("Node limit must be at least 8");
        this.nodeLimit = nodeLimit;
    }

    public Graph build(Dataset dataset, Conversation conversation) {
        Objects.requireNonNull(dataset);
        Objects.requireNonNull(conversation);
        Build build = new Build(dataset, conversation.title());
        List<EntryKey> roots = findRoots(dataset, conversation, build.diagnostics);
        for (EntryKey root : roots) {
            SemanticContext context = new SemanticContext(null, "root:" + root);
            build.schedule(root, context, null, null, "", 0);
        }
        return build.finish();
    }

    public Graph buildContinuation(Dataset dataset, Continuation continuation) {
        Objects.requireNonNull(continuation);
        Build build = new Build(dataset, continuation.title);
        build.diagnostics.add("Continuation at " + continuation.target + ": " + continuation.reason
            + ". Preserved " + continuation.context.depth + " preceding semantic barriers.");
        
        
        build.schedule(continuation.target, continuation.context, null, null, "", 0);
        return build.finish();
    }

    

    private static List<EntryKey> findRoots(Dataset dataset, Conversation conversation, List<String> diagnostics) {
        Map<Integer,Entry> entries = conversation.entries();
        var starts = new ArrayList<Entry>();
        var incoming = new HashSet<Integer>();
        for (Entry entry : entries.values()) {
            if (entry.title().trim().equalsIgnoreCase("START")) starts.add(entry);
            for (Link link : entry.links())
                if (link.target().conversationId() == conversation.id()) incoming.add(link.target().dialogueId());
        }
        var candidates = new ArrayList<Entry>(starts);
        for (Entry entry : entries.values())
            if (!incoming.contains(entry.key().dialogueId()) && !starts.contains(entry)) candidates.add(entry);
        candidates.addAll(entries.values());
        var visited = new HashSet<EntryKey>();
        var roots = new ArrayList<EntryKey>();
        int primaryReachable = 0;
        for (Entry candidate : candidates) {
            if (visited.contains(candidate.key())) continue;
            roots.add(candidate.key());
            var pending = new ArrayDeque<EntryKey>();
            pending.add(candidate.key());
            while (!pending.isEmpty()) {
                EntryKey key = pending.removeFirst();
                if (!visited.add(key)) continue;
                Entry entry = dataset.entry(key);
                if (entry != null) for (Link link : entry.links()) pending.addLast(link.target());
            }
            
            
            if (roots.size() == 1) primaryReachable = (int) visited.stream()
                .filter(k -> k.conversationId() == conversation.id() && entries.containsKey(k.dialogueId())).count();
        }
        if (starts.isEmpty() && !entries.isEmpty())
            diagnostics.add("No explicit START entry. Showing source roots/components without inventing an entry route.");
        if (roots.size() > 1)
            diagnostics.add((entries.size() - primaryReachable) + " source entries are outside the first root's reachability. "
                + "They remain visible as disconnected source components; component roots: " + roots + ".");
        if (entries.isEmpty()) diagnostics.add("This conversation contains no dialogue entries.");
        return roots;
    }

    private record Occurrence(EntryKey source, SemanticContext context, int choiceOrder) {}
    private record Extension(SemanticContext parent, String token) {}
    private record Trail(EntryKey source, SemanticContext context, String head, Trail parent) {}
    private record Pending(Occurrence occurrence, String head, Trail parent) {}
    private record Box(Kind kind, String title, String text, String metadata) {}

    private final class Build {
        private final Dataset dataset;
        private final String title;
        private final List<Node> nodes = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final List<String> diagnostics = new ArrayList<>();
        private final Deque<Pending> pending = new ArrayDeque<>();
        private final Map<Occurrence,String> heads = new HashMap<>();
        private final Map<Extension,SemanticContext> contexts = new HashMap<>();
        private final Map<EntryKey,Semantics.Analysis> analyses = new HashMap<>();
        private final Set<EntryKey> missing = new HashSet<>();
        private final Set<EntryKey> unknown = new HashSet<>();
        private int nextId, chunkPortals, changingLoops, stableLoops, reconvergences;

        Build(Dataset dataset, String title) { this.dataset = dataset; this.title = title; }
        private String id() { return "d" + nextId++; }

        private void schedule(EntryKey key, SemanticContext context, Trail trail, String from, String label, int choiceOrder) {
            Entry entry = dataset.entry(key);
            int ordinal = entry != null && isChoice(entry) ? choiceOrder : 0;
            for (Trail ancestor = trail; ancestor != null; ancestor = ancestor.parent()) {
                if (!ancestor.source().equals(key)) continue;
                if (ancestor.context() == context) {
                    edges.add(new Edge(from, ancestor.head(), append(label, "loop / same semantic context"), true));
                    stableLoops++;
                } else {
                    String portalId = id();
                    addPortal(portalId, key, context, "RE-EVALUATE LOOP", "The source returns to " + key
                        + " after conditions, effects, or unresolved commands. Open to inspect another iteration with this history.");
                    if (from != null) edges.add(new Edge(from, portalId, append(label, "loop / changed context"), false));
                    changingLoops++;
                }
                return;
            }
            Occurrence occurrence = new Occurrence(key, context, ordinal);
            String existing = heads.get(occurrence);
            if (existing != null) {
                if (from != null) edges.add(new Edge(from, existing, label, false));
                reconvergences++;
                return;
            }
            String head = id();
            heads.put(occurrence, head);
            pending.addLast(new Pending(occurrence, head, trail));
            if (from != null) edges.add(new Edge(from, head, label, false));
        }

        private Graph finish() {
            while (!pending.isEmpty()) {
                if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("Dialogue construction cancelled");
                Pending next = pending.removeFirst();
                EntryKey key = next.occurrence().source();
                Entry entry = dataset.entry(key);
                if (entry == null) {
                    nodes.add(new Node(next.head(), Kind.NOTICE, "UNRESOLVED SOURCE LINK", "Missing conversation/dialogue " + key,
                        "The outgoing source link is preserved, but this exact destination is absent from the loaded Sordland dump.",
                        "Unresolved", "", null, null, key, null));
                    missing.add(key);
                    continue;
                }
                Semantics.Analysis analysis = analyses.computeIfAbsent(key, ignored -> Semantics.analyze(entry.script(), entry.sequence()));
                List<Box> boxes = boxes(entry, analysis, next.occurrence().choiceOrder(), next.occurrence().context());
                
                
                
                if (!nodes.isEmpty() && nodes.size() + pending.size() + boxes.size() + entry.links().size() > nodeLimit) {
                    addPortal(next.head(), key, next.occurrence().context(), "CONTINUE GRAPH", "This chunk is bounded at " + nodeLimit
                        + " boxes. Open this exact source destination to continue; no route has been discarded.");
                    chunkPortals++;
                    continue;
                }
                if (nodes.isEmpty() && boxes.size() + pending.size() + entry.links().size() > nodeLimit)
                    diagnostics.add("A single source occurrence exceeds the chunk target. It is expanded atomically so opening its continuation always advances.");
                String previous = null;
                for (int i = 0; i < boxes.size(); i++) {
                    Box box = boxes.get(i);
                    String nodeId = i == 0 ? next.head() : id();
                    nodes.add(new Node(nodeId, box.kind(), box.title(), box.text(), box.metadata(), "Dialogue", entry.speaker(),
                        null, null, key, null, next.occurrence().context()));
                    if (previous != null) edges.add(new Edge(previous, nodeId, box.kind() == Kind.CONDITION ? "" : "", false));
                    previous = nodeId;
                }
                SemanticContext outgoing = next.occurrence().context();
                if (!entry.condition().isBlank()) outgoing = extend(outgoing, "condition@" + key + ":" + entry.condition());
                for (String barrier : analysis.barriers()) outgoing = extend(outgoing, "command@" + key + ":" + barrier);
                if (!analysis.unknown().isEmpty()) unknown.add(key);
                Trail ancestry = new Trail(key, next.occurrence().context(), next.head(), next.parent());
                List<Link> links = new ArrayList<>(entry.links());
                links.sort(Comparator.comparingInt(Link::order));
                for (Link link : links) {
                    Entry target = dataset.entry(link.target());
                    String label = "";
                    if (target != null && isChoice(target)) label = "Choice " + (link.order() + 1);
                    if (target != null && !target.condition().isBlank()) label = append(label, "predicate must hold");
                    if (!link.priority().isBlank() && !link.priority().equalsIgnoreCase("Normal"))
                        label = append(label, "priority " + link.priority());
                    if (link.target().conversationId() != key.conversationId()) label = append(label, "to conversation " + link.target().conversationId());
                    if (analysis.terminal()) label = append(label, "source link after End()");
                    schedule(link.target(), outgoing, ancestry, previous, label, target != null && isChoice(target) ? link.order() + 1 : 0);
                }
            }
            if (!missing.isEmpty()) diagnostics.add("Exact linked destinations missing from the supplied dump: " + missing);
            if (!unknown.isEmpty()) diagnostics.add(unknown.size() + " source entries contain unresolved commands. These are visible and prevent semantic merging.");
            if (reconvergences > 0) diagnostics.add(reconvergences + " repeated arrivals share the exact source entry and semantic history.");
            if (stableLoops > 0) diagnostics.add(stableLoops + " unchanged-context loop connections are shown as back-references.");
            if (changingLoops > 0) diagnostics.add(changingLoops + " loops cross semantic barriers; explicit re-evaluation portals preserve their context.");
            if (chunkPortals > 0) diagnostics.add(chunkPortals + " continuation portals preserve the unexpanded frontier at the " + nodeLimit + "-box chunk boundary. Click any portal to continue.");
            diagnostics.add("Predicates are preserved in source order and are not evaluated. Separate predicates do not imply an invented IF/ELSE chain. End()/output denotes conversation control, not a campaign ending.");
            return new Graph(title, nodes, markAdditionalBackEdges(nodes, edges), diagnostics);
        }

        private SemanticContext extend(SemanticContext parent, String token) {
            return contexts.computeIfAbsent(new Extension(parent, token), key -> new SemanticContext(parent, token));
        }

        private void addPortal(String id, EntryKey key, SemanticContext context, String caption, String reason) {
            Entry entry = dataset.entry(key);
            String targetTitle = entry == null ? key.toString() : entry.title();
            Continuation continuation = new Continuation(key, context, title, reason);
            nodes.add(new Node(id, Kind.REFERENCE, caption, targetTitle + "\nOpen continuation →",
                "Exact target: " + key + "\n" + reason + "\nPreserved semantic barriers: " + context.depth,
                "Continuation", "", null, null, key, continuation));
        }
    }

    private static List<Box> boxes(Entry entry, Semantics.Analysis analysis, int choiceOrder, SemanticContext context) {
        var result = new ArrayList<Box>();
        String metadata = metadata(entry, analysis, context);
        if (!entry.condition().isBlank()) result.add(new Box(Kind.CONDITION, "CONDITION", Semantics.conditionDisplay(entry.condition()), metadata));
        String text = entry.text().isBlank() ? entry.menuText() : entry.text();
        String rawTitle = entry.title().trim();
        boolean pureControl = text.isBlank();
        boolean bareEnd = pureControl && analysis.terminal()
            && analysis.commands().stream().allMatch(c -> c.kind() == CommandKind.COSMETIC || c.kind() == CommandKind.TERMINAL);
        boolean hasSemanticBox = !entry.condition().isBlank() || analysis.commands().stream().anyMatch(c -> c.kind() != CommandKind.COSMETIC);
        
        
        if (!pureControl || !hasSemanticBox || rawTitle.equalsIgnoreCase("START") || rawTitle.equalsIgnoreCase("input") || rawTitle.equalsIgnoreCase("output")) {
            Kind kind;
            String caption;
            if (pureControl) {
                kind = rawTitle.equalsIgnoreCase("output") ? Kind.TERMINAL : Kind.CONTROL;
                caption = rawTitle.isBlank() ? "CONNECTOR" : rawTitle;
                text = rawTitle.equalsIgnoreCase("output") ? "Conversation output" : "";
            } else if (entry.isPlayer()) { kind = Kind.CHOICE; caption = choiceOrder > 0 ? "YOU · " + choiceOrder : "YOU"; }
            else if (entry.isNarrator()) { kind = Kind.NARRATOR; caption = "NARRATOR"; }
            else { kind = Kind.CHARACTER; caption = entry.speaker().isBlank() ? "UNKNOWN SPEAKER" : entry.speaker(); }
            result.add(new Box(kind, caption, text, metadata));
        }
        for (Semantics.Command command : analysis.commands()) {
            if (command.kind() == CommandKind.COSMETIC) continue;
            if (command.kind() == CommandKind.EFFECT)
                result.add(new Box(Kind.EFFECT, "EFFECT · " + command.origin(), Semantics.conditionDisplay(command.raw()), metadata));
            else if (command.kind() == CommandKind.UNKNOWN)
                result.add(new Box(Kind.NOTICE, "UNRESOLVED COMMAND · " + command.origin(), command.raw(), metadata));
            else result.add(new Box(Kind.TERMINAL, "End()", "Conversation control terminal", metadata));
        }
        if (result.isEmpty()) result.add(new Box(Kind.CONTROL, rawTitle.isBlank() ? "CONNECTOR" : rawTitle, "", metadata));
        if (entry.links().isEmpty() && !bareEnd && !rawTitle.equalsIgnoreCase("output"))
            result.add(new Box(Kind.TERMINAL, "NO OUTGOING SOURCE LINKS", "This source route stops here; campaign outcome is not inferred.", metadata));
        return result;
    }

    private static boolean isChoice(Entry entry) { return entry.isPlayer() && (!entry.text().isBlank() || !entry.menuText().isBlank()); }
    private static String append(String a, String b) { return a.isBlank() ? b : a + " · " + b; }

    private static String metadata(Entry entry, Semantics.Analysis analysis, SemanticContext context) {
        return "Source conversation/dialogue: " + entry.key() + "\nActor: " + entry.actorId() + " · " + entry.speaker()
            + "\nSource title: " + entry.title() + "\nCondition (original): " + entry.condition()
            + "\nUser script (original):\n" + entry.script() + "\nSequence (original):\n" + entry.sequence()
            + "\nSource outgoing links (order/priority/connector retained): " + entry.links()
            + "\nSemantic history barriers on arrival: " + context.depth
            + "\nKnown presentation commands: " + analysis.cosmetic()
            + "\nUnknown commands: " + analysis.unknown();
    }

    

    private static List<Edge> markAdditionalBackEdges(List<Node> nodes, List<Edge> edges) {
        Map<String,List<Integer>> outgoing = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) if (!edges.get(i).back)
            outgoing.computeIfAbsent(edges.get(i).from, ignored -> new ArrayList<>()).add(i);
        Map<String,Integer> color = new HashMap<>();
        Set<Integer> back = new HashSet<>();
        record Frame(String node, Iterator<Integer> links) {}
        for (Node node : nodes) {
            if (color.containsKey(node.id)) continue;
            var stack = new ArrayDeque<Frame>();
            color.put(node.id, 1);
            stack.push(new Frame(node.id, outgoing.getOrDefault(node.id, List.of()).iterator()));
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (!frame.links().hasNext()) { color.put(frame.node(), 2); stack.pop(); continue; }
                int index = frame.links().next();
                String target = edges.get(index).to;
                if (color.getOrDefault(target, 0) == 1) back.add(index);
                else if (!color.containsKey(target)) {
                    color.put(target, 1);
                    stack.push(new Frame(target, outgoing.getOrDefault(target, List.of()).iterator()));
                }
            }
        }
        var result = new ArrayList<Edge>(edges.size());
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            result.add(back.contains(i) ? new Edge(edge.from, edge.to, append(edge.label, "back-reference"), true) : edge);
        }
        return result;
    }
}
