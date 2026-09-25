# Validation record

## Current revision: verification in progress

Command, from the project folder:

```sh
./scripts/test.sh
```

The canonical-source redesign, actor filtering and new connector routing are undergoing their complete regression pass. Final counts, timing and pass status will be recorded after that run. The earlier result of 976,850 boxes and 140,916 continuation endpoints measured the superseded state-history/chunk implementation and is not a current graph size or acceptance result.

The revised verification covers:

- Building all 286 complete conversation graphs, with one representation per reachable source identity and no continuation portals.
- Exact outgoing-link endpoints, order, priority and cross-conversation calls/returns; conditions, effects and unknown commands retained.
- A synthetic diamond reconverging at one destination with or without effects, conditions or unknown commands.
- Both state-changing and ordinary loops becoming source back-references without repeated expansion.
- Actor normalization, all-selected defaults, independent speech filtering, retained mechanics and connected projected paths.
- Unique connector selection, previous-highlight replacement, ambiguity preservation, endpoint/diagonal/zero-length segments, shared trunks and zoom-consistent hit tolerance.
- Measured node layout, rectangle overlap, connector intersections with node interiors, and source choice ordering.
- Correct campaign turn containment before and after metadata expansion, viewport transforms, schema failures and source integrity.

These are the verification targets for this revision; this list is not a claim that the full pass has completed.

## Source integrity

Reference SHA-256 values of the bundled files from the supplied archive:

| File | SHA-256 |
| --- | --- |
| `SuzerainDataDumper.conversations_Sordland.json` | `e74761760ae80f14b8445c01450f8a03d638b4c1713b2e46816ab3c7f0440531` |
| `SuzerainDataDumper.entity_data.json` | `b2aaa87c97cce245ecbbe0fe73f5bfdc1dac0c77186766e354cdc88d1ced523e` |

The regression suite compares source bytes before and after loading and graph work. A current archive-integrity confirmation will be recorded with the final verification results. The application does not modify source JSON. No new source databases or runtime dependencies are required.

## Desktop verification remains separate

No successful interactive UI or screenshot run is claimed for this revision. A prior graphical smoke attempt in the development sandbox could not obtain a screen and failed in `Screen.getMainScreen`. Headless font measurement and geometry checks can run without a visible application window.

Visual styling, physical mouse clicks, actor-checkbox interaction, file choosers and complete Back navigation through the actual window still need a desktop session with a display. Automated model, geometry and selection checks do not replace that interactive pass.

The optional `--smoke=/absolute/folder` command requires a display, has a 60-second timeout and reports failure with a nonzero exit code. It is not part of a claim that interactive verification succeeded.

## Source limitation

The supplied archive contains generated HTML/JavaScript presentation files and two JSON dumps, not the campaign scheduler. Known activation predicates and turn placement do not establish event execution order. Campaign progression remains explicitly unresolved. No total campaign route, condition solver or ordered event IF/ELSE chain is fabricated.
