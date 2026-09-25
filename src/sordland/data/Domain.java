package sordland.data;

import java.util.*;

                                                                                        
public final class Domain {
    private Domain() {}

    public record EntryKey(int conversationId, int dialogueId) {
        @Override public String toString() { return conversationId + ":" + dialogueId; }
    }
    public record Link(EntryKey target, int order, String priority, boolean connector) {}
    public record Entry(EntryKey key, int actorId, String speaker, String title,
                        String text, String menuText, String condition, String script,
                        String sequence, List<Link> links, Map<String,Object> raw) {
        public Entry { links = List.copyOf(links); raw = immutableMap(raw); }
        public boolean isPlayer() { return actorId == 5 || actorId == 6 || speaker.equals("Player") || speaker.equals("Player_Italic"); }
        public boolean isNarrator() { return actorId == 10 || speaker.equalsIgnoreCase("Narrator"); }
    }
    public record Conversation(int id, String title, Map<Integer,Entry> entries) {
        public Conversation { entries = immutableMap(entries); }
    }
    public record Option(String title, String condition, String instruction) {}
    public record Item(String id, String type, String title, String internalName,
                       String path, Integer turn, Integer conversationId, String condition,
                       String beginInstruction, String endInstruction, String description,
                       List<Option> options, Map<String,Object> raw) {
        public Item { options = List.copyOf(options); raw = immutableMap(raw); }
    }
    public record Dataset(List<Item> items, Map<Integer,Conversation> conversations,
                          List<Item> ancillary, List<String> diagnostics) {
        public Dataset {
            items = List.copyOf(items); conversations = immutableMap(conversations);
            ancillary = List.copyOf(ancillary); diagnostics = List.copyOf(diagnostics);
        }
        public Entry entry(EntryKey key) {
            Conversation c = conversations.get(key.conversationId());
            return c == null ? null : c.entries().get(key.dialogueId());
        }
    }
    private static <K,V> Map<K,V> immutableMap(Map<K,V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
