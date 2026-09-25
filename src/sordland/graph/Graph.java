package sordland.graph;

import sordland.data.Domain.EntryKey;
import sordland.data.Domain.Item;
import sordland.data.Domain.Link;
import java.util.*;


public final class Graph {
    public enum Kind { EVENT, CONDITION, EFFECT, CHARACTER, NARRATOR, CHOICE, CONTROL, TERMINAL, REFERENCE, NOTICE, JUNCTION }

    
    public record CampaignMetadata(List<CampaignTurn> turns, List<CampaignLevel> levels, List<CampaignGroup> groups) {
        public CampaignMetadata { turns=List.copyOf(turns); levels=List.copyOf(levels); groups=List.copyOf(groups); }
    }
    public record CampaignTurn(int turn, int sourceIndex, String title, String entryId) {}
    public record CampaignLevel(int turn, int turnSourceIndex, int stepIndex, String entryId, String exitId,
                                List<String> eventIds, List<String> conditionIds, List<String> groupIds) {
        public CampaignLevel { eventIds=List.copyOf(eventIds); conditionIds=List.copyOf(conditionIds); groupIds=List.copyOf(groupIds); }
    }
    
    public record CampaignGroup(String id, String entryId, String exitId, List<String> eventIds) {
        public CampaignGroup { eventIds=List.copyOf(eventIds); }
    }

    public static final class Node {
        public final String id, title, text, metadata, type, speaker;
        
        public final String actor;
        public final Kind kind;
        public final Integer turn;
        public final Item item;
        public final EntryKey source;

        public Node(String id, Kind kind, String title, String text, String metadata,
                    String type, String speaker, Integer turn, Item item, EntryKey source) {
            this(id, kind, title, text, metadata, type, speaker, turn, item, source, "");
        }
        public Node(String id, Kind kind, String title, String text, String metadata,
                    String type, String speaker, Integer turn, Item item, EntryKey source, String actor) {
            this.id = Objects.requireNonNull(id);
            this.kind = Objects.requireNonNull(kind);
            this.title = Objects.requireNonNullElse(title, "");
            this.text = Objects.requireNonNullElse(text, "");
            this.metadata = Objects.requireNonNullElse(metadata, "");
            this.type = Objects.requireNonNullElse(type, "");
            this.speaker = Objects.requireNonNullElse(speaker, "");
            this.actor = Objects.requireNonNullElse(actor, "");
            this.turn = turn;
            this.item = item;
            this.source = source;
        }
    }

    public static final class Edge {
        public final String from, to, label;
        public final boolean back;
        
        public final EntryKey sourceFrom, sourceTo;
        public final Link sourceLink;
        public final List<Edge> projectionPath;
        public Edge(String from, String to, String label, boolean back) {
            this(from, to, label, back, null, null);
        }
        public Edge(String from, String to, String label, boolean back, EntryKey sourceFrom, Link sourceLink) {
            this(from, to, label, back, sourceFrom, sourceLink, List.of());
        }
        public Edge(String from, String to, String label, boolean back, List<Edge> projectionPath) {
            this(from, to, label, back, null, null, projectionPath);
        }
        private Edge(String from, String to, String label, boolean back, EntryKey sourceFrom, Link sourceLink, List<Edge> projectionPath) {
            this.from = Objects.requireNonNull(from);
            this.to = Objects.requireNonNull(to);
            this.label = Objects.requireNonNullElse(label, "");
            this.back = back;
            this.sourceFrom = sourceFrom;
            this.sourceLink = sourceLink;
            this.sourceTo = sourceLink == null ? null : sourceLink.target();
            this.projectionPath = List.copyOf(projectionPath);
        }
        public Edge withBack(boolean back) { return this.back == back ? this : new Edge(from, to, label, back, sourceFrom, sourceLink, projectionPath); }
    }

    public final String title;
    public final List<Node> nodes;
    public final List<Edge> edges;
    public final List<String> diagnostics;
    
    public final CampaignMetadata campaign;

    public Graph(String title, List<Node> nodes, List<Edge> edges, List<String> diagnostics) {
        this(title,nodes,edges,diagnostics,null);
    }
    public Graph(String title, List<Node> nodes, List<Edge> edges, List<String> diagnostics, CampaignMetadata campaign) {
        this.title = Objects.requireNonNullElse(title, "");
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.diagnostics = List.copyOf(diagnostics);
        this.campaign = campaign;
    }
}
