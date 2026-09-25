# Sordland Tree Viewer

A local JavaFX viewer built from the supplied Suzerain Sordland entity and conversation dumps. The original JSON files are read-only inputs. No Maven, Gradle, external JSON library, network service, or downloaded portrait is required.

## Run in IntelliJ IDEA

1. Open this folder as a project.
2. In **File → Project Structure → Project SDK**, choose your existing **BellSoft Liberica JDK 25 FULL** installation. The FULL distribution includes JavaFX. If necessary, add its installation directory as a JDK first.
3. Select the **Sordland Tree Viewer** run configuration and run it.

The checked-in configuration sets the project directory as the working directory, enables `javafx.controls`, and explicitly permits the bundled JavaFX graphics module to load its native libraries. All three supplied files are included in `data/` under their canonical names. The entity and conversation files load automatically; the actor-name file is bundled and validated by headless tests without replacing dialogue speaker inference. If an input file is missing, the application offers a file chooser and checks its contents.

## Run from a terminal

On macOS or Linux, from this project folder:

```sh
./scripts/run.sh
```

On Windows:

```bat
scripts\run.cmd
```

Set `JAVA_HOME` to an existing Liberica JDK 25 FULL installation when needed. The Unix scripts also recognize the existing local JDK used for development; otherwise they try `javac` on `PATH`. The Windows scripts try `PATH` when `JAVA_HOME` is unset. The scripts never install or download a runtime. Compilation outputs stay in `build/`.

To compile without opening the application, use `./scripts/build.sh` or `scripts\build.cmd`.

To use another existing data folder:

```sh
./scripts/run.sh --data=/absolute/path/to/data
```

The JVM property `-Dsuzerain.data=/absolute/path/to/data` is also supported. Use **Data files…** in the application to select replacement inputs. Invalid or cancelled inputs leave a recovery action available; the viewer never writes to these files.

## Controls

- Click the body of a campaign item to open its dialogue graph or source-defined detail view.
- Click the small right-hand button to expand or collapse campaign metadata.
- Use the mouse wheel or **− / +** to zoom. Drag empty canvas space to pan. **Fit** frames the current view.
- **Back** returns to the retained view, including pan, zoom, metadata expansion, selection, and search text. Metadata expansion keeps the selected node anchored.
- **Readable** centers the selected node at 100% zoom. **Find** searches the current graph; press it again to move through successive matches.
- Click a dialogue node to inspect its source fields. **Item details** exposes the catalogue condition and begin/end instructions alongside a dialogue view.
- In a dialogue view, toggle actors independently in the checkbox list. All actors start selected. Hidden speech is bypassed visually; conditions, effects and other source mechanics remain visible. The choices persist for that view through relayout and Back navigation.
- Click a connector away from nodes to highlight it in green and inspect its endpoints. Only one connector is highlighted. A click near multiple connectors is ignored and keeps the current selection.
- Parsing and layout run in the background; use **Cancel** to return to the previous view.
- **Event view: ROOTED** is the startup default. Click the same button to switch to **Event view: PLAIN** and back. The toggle is available only at campaign level.
- ROOTED starts at synthetic **START** and follows the complete Sordland GameFlow. Selecting one turn uses **TURN START**; All turns restores the continuous graph.
- In ROOTED, **Find** highlights matching events and cycles focus without removing intermediary nodes. Type filters dim nonmatching events; ancillary data is excluded. PLAIN retains the original catalogue filtering and ancillary checkbox.
- **Speaker Colors** assigns stable colors to character speakers. Narrator and player choices retain their fixed styles.

## Reading the graphs

Conditions use light pink boxes with a purple border; gameplay effects have a dedicated style. Only the literal `BaseGame.` prefix is removed for display. Raw expressions and source identifiers remain available in metadata.

Each exact source entry `(conversationID, dialogueID)` has one representation in a dialogue graph. Its condition, speech/control, effects, unknown commands and terminal markers remain in source order. Every outgoing source link points to that destination's single representation, including links from branches with different effects. The viewer displays the JSON pointer graph; it does not calculate whether a particular accumulated game state can take a route.

The complete source graph is built without semantic-history copies or continuation pages. Repeated destinations reconverge, and source loops connect back to the existing entry. Player boxes say **YOU**; incoming edges carry **Choice 1**, **Choice 2**, and so on in source order. Dialogue conditions are displayed exactly and never evaluated or combined into inferred IF/ELSE branches. Campaign conditions can share a true/false split only when a strict parser proves complementarity.

Dialogue connectors use separate ordered exit ports, destination ports and staggered channels. Long connectors and back-references route around node rectangles. Gaps or halos at unrelated crossings distinguish crossing lines from junctions. Arrowheads show direction; source back-references are dashed. A selected connector and its arrowhead use a thicker green stroke.

Actor filtering projects this same source graph. It hides only character speech, narration and player-choice boxes. Remaining paths stay connected, with compact reference junctions where needed to preserve hidden branches or loops. The full graph supplies the actor list, so hiding an actor never removes their checkbox.

**PLAIN** is the original flat source catalogue: all campaign entities, additional dialogue graphs, and optional ancillary records. Its activation lines do not assert execution order. The existing cards and item details remain available.

**ROOTED** uses only `GameFlowData` for `StoryPack_Main`. Numeric source indices give authoritative **Turn → Step → Fragment** order. Each step is a progression level; exact `NameInDatabase` matches resolve its fragments. The graph remains continuous across horizontal turn separators, which use `TransitionTitle` where present.

`StoryFragmentCondition` appears in a separate pink **CONDITION** block, with the original expression retained in metadata. A small, conservative expression parser can combine proven complementary predicates into true/false paths. Other predicates remain separate. A direct decision branch is shown only when choices mechanically prove activation of the immediately following level, such as the Turn 3 Gasom extraction choice. Matching a variable across distant events never creates an edge.

Neutral junctions communicate progression and reconvergence whenever GameFlow does not prove a specific event parent. They are drawing mechanisms, not clickable campaign events. Tight gray rounded boxes enclose multiple unconditional events at the same step, without asserting an order between them. These boxes have no title, metadata or click behavior; each enclosed event remains clickable. Conditional events stay on their own branches. Missing identifiers become explicit **UNRESOLVED FRAGMENT** nodes at their source positions and receive diagnostics.

Turn conditions and start instructions, step instructions, and original indices remain inspectable as source metadata. Scripts and conditions are never executed. See [data findings and limits](docs/DATA_DISCOVERY.md) for the evidence.

## Checks

```sh
./scripts/test.sh
```

Windows: `scripts\test.cmd`. IntelliJ: **Headless Checks** run configuration. Checks use explicit assertions without a testing framework or a visible JavaFX application window. Checks cover the supplied GameFlow and synthetic edge cases, rooted source coverage and branch proofs, compact groups, acyclicity, deterministic layout, rectangle and connector collisions, PLAIN compatibility, Decision/Bill source options, and all 286 existing complete dialogue graphs. Dialogue regression tests are preserved. Confirmed results are recorded in the validation document.

See [validation results and remaining limits](docs/VALIDATION.md) and [changes in this iteration](docs/CHANGES.md).

An optional graphical smoke run creates local screenshots and exits:

```sh
./scripts/run.sh --smoke=/absolute/path/to/screenshot-folder
```

This requires a graphical display. It fails with a nonzero exit code if it cannot complete within 60 seconds. The smoke run checks ROOTED startup, toggling both ways, isolated turns, structure-preserving filters, event detail/dialogue callbacks and Back navigation. A graphical session is required; see the validation record for this environment’s result.

## Project structure

| Path | Purpose |
| --- | --- |
| `src/` | JSON parser, domain records, semantic analysis, graphs, layout and JavaFX UI |
| `data/` | Unmodified supplied Sordland source JSON files |
| `tests/` | Headless regression and real-data checks |
| `scripts/` | JDK-only build, launch and test scripts |
| `.idea/`, `.run/`, `SordlandTreeViewer.iml` | Normal IntelliJ project and run configurations |
| `build/` | Generated compilation and test outputs |

The application is an inspection tool. It does not execute game scripts, evaluate a playthrough's full game state, change saves, or modify source databases.
