package sordland.analysis;

import java.util.*;

                                                                                
public final class VariableCatalog {
    private final VariableIndex index;
    public VariableCatalog(VariableIndex index) { this.index = index; }
    public List<String> filter(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return index.variables().stream().filter(name -> (name + " " + index.label(name)).toLowerCase(Locale.ROOT).contains(normalized))
            .sorted(Comparator.comparingInt((String name) -> rank(name.toLowerCase(Locale.ROOT), normalized)).thenComparing(Comparator.naturalOrder())).toList();
    }
    private static int rank(String name, String query) {
        return name.equals(query) ? 0 : name.startsWith(query) ? 1 : Arrays.stream(name.split("[._]")).anyMatch(piece -> piece.startsWith(query)) ? 2 : 3;
    }
}
