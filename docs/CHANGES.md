# Canonical dialogue graph, actor filtering and connector selection

This revision replaces the earlier expansion by route history. Earlier counts of duplicated occurrences and continuation endpoints describe the superseded design.

## Source graph

- Each `(conversationID, dialogueID)` is represented once per dialogue graph. Different incoming effects, conditions or choice numbers do not duplicate the destination.
- Conditions, speech/control, effects, unresolved commands and terminal boxes remain visible in source order within each entry.
- Exact outgoing links, their order, priority and connector metadata remain authoritative, including cross-conversation calls and returns.
- Player nodes use the caption **YOU**. Choice numbers belong to incoming edges.
- Both state-changing and other source loops return to the existing entry and use back-reference connectors.
- Removed semantic-context interning, route-history inspection, changed-context loop portals and the old continuation/chunk architecture. Graphs contain their complete source routes.

## Actor filtering

- Dialogue views offer an independent JavaFX checkbox for each actor in the complete graph. All begin selected; player actors 5 and 6 share **You**, and narration uses **Narrator**.
- Hiding an actor removes only CHARACTER, NARRATOR and CHOICE speech boxes. Conditions, effects, unknown commands, control boxes and terminal information remain.
- Filtering is a visual projection over the source graph. Routes bypass hidden speech; compact reference junctions preserve hidden branching and loops when required.
- Actor choices belong to the retained view and survive relayout and Back navigation. Opening another event starts with all of its actors selected.

## Connectors

- Ordered source ports, separate destination ports and staggered gap channels keep outgoing routes distinguishable. Long links and loops route around node rectangles; unrelated crossings use visual gaps or halos.
- A unique connector click highlights that edge and arrowhead in thicker accent green and shows endpoint details. Node clicks retain priority.
- Hit-testing uses polyline segment distance with a six-screen-pixel tolerance. It deduplicates by edge identity before deciding whether a hit is unique.
- Shared trunks, overlapping connectors and intersections never cause an arbitrary selection. Ambiguous and empty clicks preserve the current highlight. Selecting another edge replaces it; filtering away the selected edge clears it.
- Geometry and selection logic live in JavaFX-independent helpers for headless regression tests.

## Retained behavior and scope

Turn sectors, independent campaign predicates, source-driven metadata, detail views for non-dialogue items, Speaker Colors, search, zoom, pan, Fit, Readable, loading recovery and background work remain part of the viewer. Source databases stay read-only. The project still uses the JavaFX bundled with Liberica JDK 25 FULL without Maven, Gradle or external UI dependencies.

Campaign event execution order remains unresolved in the supplied files. This revision does not infer an event scheduler, solve conditions, simulate Lua or calculate playthrough state.

## Verification

The revised complete-graph, actor-filter, routing and connector-selection checks are being run. See [VALIDATION.md](VALIDATION.md) for confirmed results and remaining desktop checks; previous chunk-based test totals are not current acceptance results.

## Routing background

Optional design references: [Graphviz spline routing](https://graphviz.org/docs/attrs/splines/) and [yFiles orthogonal edge routing](https://docs.yworks.com/yfiles/doc/developers-guide/orthogonal_edge_router.html). These are background references only. Neither Graphviz nor yFiles is a project or runtime dependency.
