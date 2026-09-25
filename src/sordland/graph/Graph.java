package sordland.graph;

import sordland.data.Domain.EntryKey;
import sordland.data.Domain.Item;
import java.util.*;


public final class Graph {
    public enum Kind { EVENT, CONDITION, EFFECT, CHARACTER, NARRATOR, CHOICE, CONTROL, TERMINAL, REFERENCE, NOTICE }

    public static final class Node {
        public final String id, title, text, metadata, type, speaker;
        public final Kind kind;
        public final Integer turn;
        public final Item item;
        public final EntryKey source;
        public final Continuation continuation;
        public final SemanticContext context;

        public Node(String id, Kind kind, String title, String text, String metadata,
                    String type, String speaker, Integer turn, Item item, EntryKey source,
                    Continuation continuation) {
            this(id, kind, title, text, metadata, type, speaker, turn, item, source, continuation,
                continuation == null ? null : continuation.context);
        }
        public Node(String id, Kind kind, String title, String text, String metadata,
                    String type, String speaker, Integer turn, Item item, EntryKey source,
                    Continuation continuation, SemanticContext context) {
            this.context = context;
            this.id = Objects.requireNonNull(id);
            this.kind = Objects.requireNonNull(kind);
            this.title = Objects.requireNonNullElse(title, "");
            this.text = Objects.requireNonNullElse(text, "");
            this.metadata = Objects.requireNonNullElse(metadata, "");
            this.type = Objects.requireNonNullElse(type, "");
            this.speaker = Objects.requireNonNullElse(speaker, "");
            this.turn = turn;
            this.item = item;
            this.source = source;
            this.continuation = continuation;
        }
    }

    public static final class Edge {
        public final String from, to, label;
        public final boolean back;
        public Edge(String from, String to, String label, boolean back) {
            this.from = Objects.requireNonNull(from);
            this.to = Objects.requireNonNull(to);
            this.label = Objects.requireNonNullElse(label, "");
            this.back = back;
        }
    }

    


    public static final class SemanticContext {
        final SemanticContext parent;
        final String token;
        public final int depth;
        SemanticContext(SemanticContext parent, String token) {
            this.parent = parent;
            this.token = Objects.requireNonNull(token);
            this.depth = parent == null ? 0 : parent.depth + 1;
        }
        public List<String> history() {
            var result = new ArrayList<String>();
            for (SemanticContext c = this; c != null; c = c.parent)
                if (!c.token.isEmpty()) result.add(c.token);
            Collections.reverse(result);
            return List.copyOf(result);
        }
    }

    
    public static final class Continuation {
        public final EntryKey target;
        public final SemanticContext context;
        public final String title, reason;
        public Continuation(EntryKey target, SemanticContext context, String title, String reason) {
            this.target = Objects.requireNonNull(target);
            this.context = Objects.requireNonNull(context);
            this.title = Objects.requireNonNullElse(title, "Continue dialogue");
            this.reason = Objects.requireNonNullElse(reason, "");
        }
    }

    public final String title;
    public final List<Node> nodes;
    public final List<Edge> edges;
    public final List<String> diagnostics;

    public Graph(String title, List<Node> nodes, List<Edge> edges, List<String> diagnostics) {
        this.title = Objects.requireNonNullElse(title, "");
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.diagnostics = List.copyOf(diagnostics);
    }
}
