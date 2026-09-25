# Validation record

## Passed

Command, from the project folder:

```sh
./scripts/test.sh
```

Latest complete run: **passed in 8.99 seconds** with the existing Liberica JDK 25 FULL runtime.

The real-data pass built the first segment of **all 286 conversations** at the application’s default 4,000-box target using **actual JavaFX font measurements**. Across those independent runs it checked 976,850 visual boxes and 140,916 explicit continuation endpoints. These totals include intentionally duplicated semantic contexts and separate conversation runs; they are not counts of unique source entries or exhaustive playthroughs.

Checks include:

- All 83,454 source dialogue entries and 249 cross-conversation links resolve.
- Player choice order is preserved left to right, including choices assigned to different layers.
- Dialogue rectangles do not overlap, and connector segments do not enter node interiors.
- Campaign rectangles stay within their proven turn sectors before and after all metadata is expanded, using real font measurements.
- A synthetic 20,000-node chain lays out without recursive stack overflow; viewport queries return the expected visible geometry.
- Flavour-only paths reconverge; state-changing, conditional, and unknown-command paths remain distinct.
- Stable loops become back-references, changed-context loops retain their history, and a finite chain remains fully accessible through continuation segments.
- A source entry larger than the chunk target makes progress instead of yielding an identical continuation forever.
- Cross-conversation calls and returns do not create false disconnected roots.
- Wheel zoom preserves its pointer anchor, Fit contains the bounds, Readable uses 100%, and viewport snapshots restore exactly.
- Invalid structures, mismatched IDs, Rizia-only input, ambiguous title references, and sparse source dictionary identities are handled explicitly.

A subsequent compilation passed after the final small UI changes to edge-label placement and file-chooser filters.

## Source integrity

Both bundled data files match the supplied archive byte for byte after the work:

| File | SHA-256 |
| --- | --- |
| `SuzerainDataDumper.conversations_Sordland.json` | `e74761760ae80f14b8445c01450f8a03d638b4c1713b2e46816ab3c7f0440531` |
| `SuzerainDataDumper.entity_data.json` | `b2aaa87c97cce245ecbbe0fe73f5bfdc1dac0c77186766e354cdc88d1ced523e` |

No dependencies or source databases were downloaded. All project changes and test outputs remained in the workspace.

## Not yet verified interactively

A graphical smoke launch was attempted in the development sandbox. JavaFX could not obtain a screen: its graphics initialization failed in `Screen.getMainScreen`. The process was stopped, and no successful screenshots were produced. Headless font measurement and geometry tests do work.

The following still need a desktop session with a display: visual styling, physical mouse interaction, file-chooser interaction, and end-to-end Back navigation through the actual window. Their underlying model/transform behavior has automated coverage, but that does not replace a successful GUI run.

The optional `--smoke=/absolute/folder` command now has a 60-second timeout and a nonzero failure exit code. A failed launch cannot leave a smoke test waiting indefinitely or be reported as a pass.

## Source limitation

The supplied archive contains generated HTML/JavaScript presentation files and two JSON dumps, not the campaign scheduler. Known activation predicates and turn placement do not prove event execution order. Campaign progression remains explicitly unresolved. No total campaign route or ordered event IF/ELSE chain is fabricated.
