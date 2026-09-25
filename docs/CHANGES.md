# Second implementation pass

## Graph correctness and layout

- Replaced dialogue leaf/subtree widths with layered ordering and horizontal separation constraints. This fixes an observed player-choice order inversion in conversation 1, including choices placed on different layers after reconvergence.
- Added explicit routing around intermediate boxes for long forward connectors and back-references. Source connectors retain direction; campaign activation connectors remain straight.
- Followed cross-conversation links when determining root reachability. A return destination is no longer mistakenly duplicated as an independent component.
- Prevented continuation loops that made no progress when one source entry exceeded the configured segment size. Such entries are now expanded atomically and the exception is reported.
- Exposed exact preceding semantic history in the source inspector. Removed redundant raw-entry text copies from every visual occurrence.

## Navigation and usability

- Retained search text and viewport state through Back navigation.
- Anchored a selected event during metadata expansion and corrected Readable to use 100% zoom.
- Added repeated-match Find navigation, Item details, Data files selection/recovery, and cancellation of background work.
- Assigned speaker colors from the complete source speaker set so the palette stays stable between graphs. Narrator and player styles remain fixed.
- Focused the source-marked starting event on initial load and displayed its start flag, dialogue reference, category and turn provenance.
- Added measured-text caching and avoided drawing off-screen text baselines inside large nodes.

## Parsing and verification

- Unified namespace display handling; unrelated names containing BaseGame remain intact.
- Preserved sparse catalogue dictionary keys, rejected Rizia-only conversation input, and left ambiguous title references unresolved instead of selecting an arbitrary conversation.
- Excluded “Jump to” control labels from speaker-name inference.
- Added cancellation checks to the JSON reader and measured layout pass.
- Extended regression checks to actual fonts and the default 4,000-box segment size across all supplied conversations.
- Configured JavaFX native access in launch configurations and added a timeout/failure exit code to graphical smoke checks.

## Scope limits

The supplied files still do not establish campaign event execution order. The campaign view is a turn-banded catalogue of independent items and activation predicates. No scheduler or event routes were fabricated. Interactive visual verification still requires a desktop display; the development sandbox exposes none to JavaFX.
