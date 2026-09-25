# Validation record — GameFlow campaign implementation

## Build and complete automated suite

The existing `./scripts/build.sh` and `./scripts/test.sh` scripts pass with the local Liberica JDK 25 FULL runtime. Final integrated run: **3,994,635 checks passed in 4.85 seconds**.

The complete suite includes:

- Exact StoryPack_Main loading: 11 turns, 132 steps, 227 fragment occurrences, zero unresolved references in the supplied source. Every source turn, step, fragment, condition, instruction and index is compared with the raw JSON.
- Missing, ambiguous, case-mismatched and repeated fragment fixtures; sparse source indices; invalid schema and duplicate Main schedules; explicit unresolved graph nodes and diagnostics.
- Immediate START → Inauguration, one continuous root, isolated TURN START, local Gasom decision → condition → event branches without a bypass, neutral fallback, safe Boolean complements and separate unrelated/numeric predicates.
- Multiple unconditional siblings, singleton exclusion, mixed levels, empty turns/steps, retained source instructions and acyclic/reachable graph coverage.
- Deterministic measured layout for the whole campaign, all metadata expanded, every isolated turn and a wrapping mixed-level fixture. No card overlaps; every visible connector is orthogonal and avoids card and group interiors. Groups have tight 14-unit bounding padding, one top-middle incoming connector and one bottom-middle outgoing connector. Containers and junctions have no click target; event/condition hit targets remain available.
- PLAIN builds and retains filtering/catalogue behavior. Decision option instructions and Bill SIGN/VETO source actions remain valid.
- All existing dialogue tests remain unchanged. All 286 complete canonical dialogue graphs build: 159,924 visual boxes and 187,600 exact outgoing source links across those graphs; the largest has 3,187 boxes. Actor filtering, source identity, reconvergence, loops, routing, connector selection, viewport and text measurement regressions pass.
- The dialogue builder, actor projection, semantic analyzer and dialogue edge router are byte-for-byte unchanged from the supplied project.

## Graphical verification limit

The graphical smoke test was attempted with `./scripts/run.sh --smoke=...`. This execution environment exposes no JavaFX display: native startup fails in `Screen.getMainScreen` / `MTLPipeline`. The desktop tool also rejected Terminal access for safety reasons. **No successful interactive application or JavaFX screenshot run is claimed.**

The smoke harness is included for a desktop run. It verifies ROOTED startup; toggling to PLAIN and back; isolated turns; search/type filters preserving graph and layout; existing Decision/Bill and dialogue click callbacks; and Back navigation. A plain Java launcher starts the 60-second smoke watchdog before JavaFX initialization; the environment-limited smoke attempt exits with code 2 rather than hanging.

Separate headless previews of the actual measured layout were inspected for Turn 1 sibling groups and Turn 3 Gasom branching. These confirm geometry and grouping, not JavaFX window interaction. Physical clicking, window rendering, file choosers and toolbar presentation still require a graphical desktop session.

## Source integrity

All three canonical files are byte-for-byte identical to the newest inputs supplied for this task. The regression suite also verifies unchanged source bytes after parsing/build/layout. Actor names remain packaged and parse-validated without changing dialogue speaker inference.

| File | SHA-256 |
| --- | --- |
| `SuzerainDataDumper.actor_names.json` | `336d59165011e37425c5c8e1322df820c9fecf8696abd591ddb08c142ae5bd5c` |
| `SuzerainDataDumper.conversations_Sordland.json` | `e74761760ae80f14b8445c01450f8a03d638b4c1713b2e46816ab3c7f0440531` |
| `SuzerainDataDumper.entity_data.json` | `884143c99773afd420d0f6fc716a8086efdd431fd543f4adb7fb8043cbf7e6ec` |

## Interpretation limits

GameFlow authoritatively establishes Turn → Step → Fragment ordering. It usually does not prove a unique event parent, so neutral junctions communicate progression. Direct branches are restricted to exact adjacent decision evidence; only strictly proven Boolean complements share an if/else block. Conditions/scripts are displayed, never executed. Gray sibling containers are visual, not campaign events. No Rizia view is implemented.
