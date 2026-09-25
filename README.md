# Sordland Tree Viewer

A local JavaFX viewer built from the supplied Suzerain Sordland entity and conversation dumps. The original JSON files are read-only inputs. No Maven, Gradle, external JSON library, network service, or downloaded portrait is required.

## Runtime database integration (2026-09-21)

The supplied working viewer remains the baseline. The three original JSON files are unchanged: the old conversation dump owns explicit dialogue links, and the old entity dump owns GameFlow, Decisions, Bills, News and the existing catalogue. `data/runtime/entity_catalog.json` selects the preferred member of each logical runtime collection. The runtime database is optional; removing its folder preserves the legacy viewer with a diagnostic.

Open **Budget Allocation of the Government of Sordland** to see the resolved funding panel at conversation 26 / entry 209. Health, Law Enforcement, Education and Military each have Increase / Maintain / Decrease choices. Each choice is one effect card; counter changes are combined visually while retaining their separate provenance. A distinct completion port retains the single exact continuation to conversation 27 / entry 1.

**Types** adds Policy, Situation, Report, Decree, Journal, Token status and Decision panel, all off by default. ROOTED attaches content only where exact source instructions prove the relationship. PLAIN exposes eligible Sordland catalogue records whether or not a rooted relationship exists. Opening a Decision panel from PLAIN shows its structured mechanics. Dialogue mechanics remain visible independently of campaign type defaults.

**Runtime sources** opens the complete preferred runtime sources and catalog, including unrendered configuration and quarantined records. Original **Ignored data** remains unchanged. Item details retain raw runtime objects, source file/index, panel/page context, instructions and counter fields. The four existing conditional-instruction records can show their richer runtime source alongside their original source.

Runtime StoryPack_Main counts match the metacard. One of the 561 tagged journal entries actually has a Rizia path and enable variable; it is retained and diagnosed, but excluded from gameplay (560 eligible journal cards). See [implementation report](docs/RUNTIME_INTEGRATION.md), [validation](docs/VALIDATION.md), and [Budget image](docs/runtime-validation/budget-full.png).

Run `./scripts/test.sh` for the complete legacy and runtime-aware headless suite. The original legacy assertions remain intact; new checks separately audit all 286 enriched graphs, panel branches, source pointers and deterministic geometry. Run `./scripts/test-runtime-ui.sh` on a desktop to reproduce the real JavaFX Budget snapshots and interaction checks.

## Run in IntelliJ IDEA

1. Open this folder as a project.
2. In **File → Project Structure → Project SDK**, choose your existing **BellSoft Liberica JDK 25 FULL** installation. The FULL distribution includes JavaFX. If necessary, add its installation directory as a JDK first.
3. Select the **Sordland Tree Viewer** run configuration and run it.

The checked-in configuration sets the project directory as the working directory, enables `javafx.controls`, and explicitly permits the bundled JavaFX graphics module to load its native libraries. All three supplied files are included in `data/` under their canonical names. The entity and conversation files load automatically; the actor-name file is loaded from the entity file’s folder and retained in Ignored data because its list positions do not prove dialogue ActorIDs. If an input file is missing, the application offers a file chooser and checks its contents.

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
- In ROOTED, **Find** highlights matching visible cards and cycles focus without removing intermediary nodes. **Types X / Y** is the same independent checklist in ROOTED and PLAIN: all applicable types start selected except News and the new runtime gameplay types. Condition is independent. Unchecked cards disappear; original routes are projected through hidden cards without merging alternatives. Search never restores hidden types. PLAIN search filters the catalogue.
- **Ignored data** opens a searchable source inspector with collapsible source-file groups, exact locations, identities, reasons and raw JSON. Supported News and conditional instructions remain normal graphical types.
- **Speaker Colors** assigns stable colors to character speakers. Narrator and player choices retain their fixed styles.

## Reading the graphs

Conditions use dark purple boxes with a purple border; gameplay effects have a dedicated style. Legacy dialogue expressions only remove the literal `BaseGame.` prefix. Runtime panel choices additionally shorten literal assignment names and arithmetic for readability. Raw expressions and source identifiers remain available in metadata.

Each exact source entry `(conversationID, dialogueID)` has one representation in a dialogue graph. Its condition, speech/control, effects, unknown commands and terminal markers remain in source order. Every outgoing source link points to that destination's single representation, including links from branches with different effects. The viewer displays the JSON pointer graph; it does not calculate whether a particular accumulated game state can take a route.

The complete source graph is built without semantic-history copies or continuation pages. Repeated destinations reconverge, and source loops connect back to the existing entry. Player boxes say **YOU**; incoming edges carry **Choice 1**, **Choice 2**, and so on in source order. Dialogue conditions are displayed exactly and never evaluated or combined into inferred IF/ELSE branches. Campaign activation conditions show TRUE and FALSE/skip routes to the authoritative step transition. A strict parser pairs complementary predicates; unknown turn-condition false destinations remain explicit unresolved notices.

Dialogue connectors use separate ordered exit ports, destination ports and staggered channels. Long connectors and back-references route around node rectangles. Gaps or halos at unrelated crossings distinguish crossing lines from junctions. Arrowheads show direction; source back-references are dashed. A selected connector and its arrowhead use a thicker green stroke.

Actor filtering projects this same source graph. It hides only character speech, narration and player-choice boxes. Remaining paths stay connected, with compact reference junctions where needed to preserve hidden branches or loops. The full graph supplies the actor list, so hiding an actor never removes their checkbox.

**PLAIN** is the original flat source catalogue: all campaign entities, additional dialogue graphs, and supported News and conditional instructions. Its activation lines do not assert execution order. The existing cards and item details remain available.

**ROOTED** uses only `GameFlowData` for `StoryPack_Main`. Numeric source indices give authoritative **Turn → Step → Fragment** order. Each step is a progression level; exact `NameInDatabase` matches resolve its fragments. The graph remains continuous across alternating full-width turn bands. Large bold vertical labels use `TransitionTitle` where present, with measured single-line ellipsis and a full-title tooltip.

`StoryFragmentCondition` appears in a separate purple **CONDITION** block, with the original expression retained in metadata. A small, conservative expression parser can combine proven complementary predicates into true/false paths. Other predicates remain separate. A direct decision branch is shown only when choices mechanically prove activation of the immediately following level, such as the Turn 3 Gasom extraction choice. Matching a variable across distant events never creates an edge.

Neutral junctions communicate progression and reconvergence whenever GameFlow does not prove a specific event parent. They are drawing mechanisms, not clickable campaign events. Tight gray rounded boxes enclose multiple unconditional events at the same step, without asserting an order between them. These boxes have no title, metadata or click behavior; each enclosed event remains clickable. Conditional events stay on their own branches. Missing identifiers become explicit **UNRESOLVED FRAGMENT** nodes at their source positions and receive diagnostics.

Turn conditions and start instructions, step instructions, and original indices remain inspectable as source metadata. Scripts and conditions are never executed. See [data findings and limits](docs/DATA_DISCOVERY.md) for the evidence.

News is available in ROOTED when an exact enable-variable write or exact `EnableNews` argument proves an association. It appears in a local annotation beside/below that source event/effect. Dialogue/choice evidence is explicitly **POSSIBLE NEWS EFFECT**, not a guaranteed outcome of completing the event. Article cards contain only a title and database identity. Full articles, newspapers, raw enable variables and source evidence remain in details/expanded metadata. Unproven articles remain available in PLAIN and are listed in ROOTED diagnostics.

The shared dark palette preserves semantic type colors. Text uses the same actual font metrics as the Canvas renderer; long text wraps or truncates without horizontal compression. Type projection retains canonical topology and original connector provenance in the edge inspector. Compatible choices persist through ROOTED/PLAIN, turn changes and Back.

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

This requires a graphical display. It fails with a nonzero exit code if it cannot complete within 60 seconds. The smoke run checks both modes, the actual checklist popup, type projection, News details, Ignored Data groups/search, turn selection, hidden-type search, event/dialogue navigation, actors, metadata, zoom and Canvas edge hit-testing. It was run successfully on the local Mac display; reviewed screenshots and results are included in the validation record.

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

## Game variable inspector

The compact search-mode menu after **Find** offers **Current view** (the unchanged
startup graph search) and **Game variable**. Game variable opens a separate,
resizable themed window. Closing it returns the menu to Current view.

Type any case-insensitive substring to filter the cached variable catalog, then
**click an exact variable option** to analyse it. Typing, highlighting a row, or
pressing Enter in the filter does not analyse free text. Runtime titles appear
alongside canonical names where available.

The inspector groups boolean and assigned-value rules, uses a modifier table for
predominantly numeric variables, and shows a small state-transition table only
when complete one-hot co-writes prove it. Related guard variables support
navigation with Back/Forward. Catalog creation and cached analyses use a separate
background worker with cancellation; loading new source files discards the old
inspector and its caches.

**EXACT** identifies a supported local operation; **EXPLICIT_STRUCTURE** identifies
an operation tied to its own encoded guard or runtime panel/choice fields.
**UNRESOLVED** appearances remain in source evidence and never become confident
rules. A source with no local guard is not proof that it is globally reachable or
unconditional. Earlier dialogue predicates are never inferred as causes.

Scripts are **never executed**, and source JSON remains read-only. Expand source
operations or evidence rows for exact expressions, full source fields, guards,
identities, proof categories, and alternate runtime locations. Source identities
are provided instead of changing the viewer's graph-navigation history.

Run `./scripts/test-variable-ui.sh` for the optional desktop inspector smoke test.
See [Variable Inspector validation](docs/VARIABLE_INSPECTOR.md) for architecture,
coverage, real-data results, and deliberate limits.
