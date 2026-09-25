# GameFlow campaign view

The supplied GameFlow now drives a continuous event graph. ROOTED is the startup default; a single campaign button switches to the preserved PLAIN catalogue and back.

- Domain records retain StoryPack, turn/step/fragment indices, conditions, transition titles, instructions and raw source metadata. Resolution uses exact `NameInDatabase`; missing or ambiguous references stay visible and diagnostic.
- The rooted builder creates START, local activation branches, reconvergence junctions and turn separators. Only proven complements share a true/false split. The adjacent Gasom extraction branches are supported by exact decision option writes and activation predicates.
- Several unconditional same-level events receive a tight, noninteractive gray container. Conditional events remain separate. Neutral junctions do not claim event causality.
- A dedicated layered layout keeps campaign steps in source order, uses compact group ports and routes between measured rectangles. Dialogue graph construction and routing remain unchanged.
- ROOTED search highlights/focuses without pruning; type filters dim events; isolated turns receive TURN START. PLAIN keeps catalogue filtering and ancillary data.
- Conversation clicks use the existing dialogue builder; Decisions and Bills use their existing detail panels. Dialogue actors, colors, source links and Back navigation retain their behavior.
- All three newest JSONs are packaged. Actor names are validated by headless checks but do not override speaker inference because list indices do not provide explicit dialogue ActorIDs.
- Headless tests cover source order, completeness, conditions and branch safety, groups, unresolved fixtures, geometry, PLAIN and details. The graphical smoke path also exercises the real campaign toggle and event-opening callbacks.

See [VALIDATION.md](VALIDATION.md) for confirmed checks and [DATA_DISCOVERY.md](DATA_DISCOVERY.md) for source interpretation.
