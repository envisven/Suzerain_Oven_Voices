# Supplied data: findings and limits

This report describes the two JSON files and the reference material in the supplied `databases.zip`. No online data or external assumptions were used. The viewer reads the source files without modifying them.

## Archive and reference viewer

The archive contains the two JSON dumps, generated `out/index.html`, generated conversation HTML pages, `style.css`, and a 61-line `script.js`. The script implements collapsible sections, anchor navigation, and display controls. The supplied archive does **not** contain the original JSON-to-HTML generator, game campaign scheduler, or an actor/portrait database.

The generated pages corroborate numeric IDs and outgoing-link destinations. For example, `conversation1.html` presents conversation ID 1 and anchors such as `DialogueEntry-3`. The index position `[0]` is a collection position, **not** conversation ID 0. Neither the index order nor the browser script supplies campaign progression logic.

## JSON representation

Both roots are objects. Most logical lists are serialized as dictionaries with numeric string keys (`"0"`, `"1"`, …) plus a `_type` marker. The loader orders those entries by their numeric keys and ignores the type marker as a list element. Conventional JSON arrays are also accepted. IDs are read from their explicit fields, not reconstructed from list position.

The parser reads UTF-8 through a buffered character reader, avoiding a second 123 MB input string. JSON structures are immutable. It rejects malformed JSON, duplicate object keys, trailing data, invalid collection shape, invalid integer IDs, duplicate dialogue identities, and links whose declared origin disagrees with their containing entry.

## Campaign catalogue and campaign filtering

| Collection | Sordland | Excluded Rizia | Role |
| --- | ---: | ---: | --- |
| `AllConversationsData` | 151 | 113 | Dialogue-bearing campaign entities |
| `AllBillsData` | 22 | 4 | SIGN/VETO interactions |
| `AllDecisionsData` | 56 | 254 | Source-defined option interactions |
| `conditionalInstructionData` | 4 | 364 | Ancillary conditional calculations |
| `NewsData` | 1,436 | 40 | Ancillary newspaper content |

There are 229 Sordland interactive catalogue entities and 1,440 ancillary records. No separate decree collection is supplied. A decree can appear as a conversation or a source command such as `ShowOneTimeDecreesPanel`; that is not evidence for an absent decree option schema.

All 229 interactive items have `Path` under `Sordland/` and `AppBundleProperties.StoryPacks` containing `StoryPack_Main`. Explicit Rizia paths or dialogue references are excluded. For records without a campaign path, the loader can use unambiguous `StoryPack_Main` membership while rejecting `StoryPack_Rizia`. All 775 excluded catalogue records in these files are Rizia records. Conversation titles must begin with `Sordland/`.

Catalogue metadata includes `NameInDatabase`, `Path`, `AssignedTokenProperties`, and `StoryFragmentProperties`. The latter contains `StoryFragmentCondition`, `OnStoryFragmentBeginInstruction`, and `OnStoryFragmentEndInstruction`.

## Turn membership and endings

Interactive catalogue items have **no numeric Turn field**. Their `NameInDatabase` normally contains `TurnNN`. The loader records that as the turn's provenance. Dialogue references and conversation titles also encode turns as `Sordland/TurnNN/...`.

All nine catalogue names beginning `Turn_Endings_` resolve to explicit **Turn 11** conversation paths. They must therefore appear in Turn 11, not an invented end-turn band. Their graphs remain accessible:

| Entity suffix | Conversation ID |
| --- | ---: |
| `A_ElectionSpeech` and `A_ElectionSpeech_MaroonSquare` | 244 |
| `A_ImpeachmentTrial` | 246 |
| `Personal_Exile` | 255 |
| `Personal_Retirement` | 254 |
| `SnO_CoupTrial` | 243 |
| `SnO_RumburgWarLost` | 242 |
| `SnO_RumburgWarWin` | 245 |
| `SnO_WorldWar` | 256 |

An actual inconsistency is retained and reported: `WIP_Turn04_EnT_RuralDevelopment` references `Sordland/Turn01/EnT_EconomicOverview` (conversation 1). The catalogue item stays in its named Turn 4, with the conflicting dialogue turn visible in diagnostics. It is not silently conflated with the ordinary Turn 1 economic overview.

Conversation-only paths with no numeric turn, including `Sordland/Prologue` and numerous `Sordland/Epilogue...` graphs, remain explicitly unplaced. Their name does not prove a numeric turn. `NewsProperties.TurnNo` is a real numeric turn field and is used for news. Conditional instructions have schedule/check metadata rather than ordinary event turn membership.

## Progression: what is and is not established

`ConversationProperties.IsOnStart` is true only for `Turn01_Start_Inauguration` (conversation 6). That establishes its start flag. It does not establish a total route through the catalogue.

Source activation predicates are present on 82 conversations, 14 bills, and 45 decisions. These are **independent predicates**. No ordered IF/ELSE IF campaign routing, next-event field, scheduler steps, or explicit event-to-event execution order is present. A variable assignment matching another item's activation condition proves a state dependency; it does not prove that the latter item runs immediately next or excludes every intervening event.

The 249 links between different conversation IDs are dialogue links, including subdialogue calls and returns. They cannot be promoted to campaign event edges. Commands such as `AdvanceTimeline` do not supply the missing scheduler. The application must mark campaign progression unresolved and avoid fabricating edges from turns, catalogue order, common flags, or cross-conversation dialogue calls. A grouped timeline/catalogue is necessarily the truthful fallback for these specific files.

## Conversation identity, text, and source order

The conversation root contains `conversations`: **286 conversations and 83,454 dialogue entries**. A conversation has `id`, `Title`, and `dialogueEntries`. Every entry has `conversationID` and `id`; its identity is the pair `(conversationID, id)`.

`ConversationProperties.Dialogue` exactly matches `Conversation.Title`. All 151 catalogue references resolve. They reference 149 unique conversations: the two election-speech entities share conversation 244, and the WIP rural-development item shares conversation 1. These distinct catalogue entities retain distinct identities, metadata, and conditions.

The remaining 137 conversation graphs are retained as dialogue-fragment/ending items, giving **366 accessible primary items**. They are not asserted to be independent campaign events.

Each `outgoingLinks` element supplies `destinationConversationID`, `destinationDialogueID`, `originConversationID`, `originDialogueID`, `priority`, and `isConnector`. The loader preserves numeric source order, exact destination identity, priority, and connector status. There are no unresolved link destinations in the supplied dump.

Entry `fields` is another numbered list of `{title, typeString, value}` objects. English spoken/narrated text is normally in the field titled `en`; `Dialogue Text` is the fallback. Player menu text is normally `Menu Text en`, with `Menu Text` as fallback. Base `Sequence` and localized `Sequence en` are both preserved. `Articy Id`, `InputId`, and `OutputId` remain available in entry metadata. To reduce memory, field objects are normalized into an immutable title-to-value map; redundant field wrapper/type objects and duplicated outgoing-link raw objects are not retained after interpretation.

## Speakers and choices

There is no actor collection or portrait asset in this archive. Speaker names are corroborated across actual dialogue titles of the form `Speaker: "text"`, indexed by `ActorID`. Control labels such as `Jump to:` and script lines containing colons must not become actor names.

The source repeatedly identifies actor 5 as `Player`, actor 6 as `Player_Italic`, and actor 10 as `Narrator`. Both player actors represent responses/actions; narrator is a distinct role. Other examples are 4 `Petr Vectern`, 38 `Lucian Galade`, and 51 `Symon Holl`. Actor 1 sometimes has the literal source label `...`; those entry-specific labels are preserved, while `Ovid Grecer` is the corroborated default for its control entries. Speaker names alone are used when no local portrait can be resolved.

Player choice order comes from the incoming source node's ordered outgoing links. It must not be derived from dialogue IDs, alphabetical text, or screen position.

## Effects, controls, conditions, and non-dialogue options

Entry `conditionsString` and `userScript` carry logical predicates and scripts. Sequence fields additionally carry presentation commands and occasional other commands; both are retained. Literal `BaseGame.` may be removed for display only; `BaseGameSupport.`, `BaseGameIsolated.`, `GameCondition.`, and every other namespace remain intact. Source expressions are not reordered or simplified.

Variable assignments and compound assignments are present directly in scripts. There are also calls such as `EnableNews`, `AddTokenStatus`, `UpdateCountryRelationship`, and `UpdateProgress` whose meaning cannot safely be equated with plain cosmetic audio/UI commands. Unknown commands must remain available for inspection. The viewer never executes source scripts.

Control titles include `START`, `input`, `output`, `Jump to: ...`, and explicit `End();`. `End()` is a dialogue/conversation control concept, not a proof of campaign completion. For example, entry `1:348` has script `End();` and an actual outgoing link to output `1:2`.

Bills use `BillProperties.Title`, `Description`, `SignVariables`, `VetoVariables`, and `IsVetoDisabledCondition`. The final field is a **disable** predicate, not a VETO availability predicate; the detail view labels it `Disabled when:` instead of silently inverting or misrepresenting it. SIGN and VETO effects remain separate.

Decisions use `DecisionProperties.Options`, an ordered list of objects with `Text`, `Condition`, and `Instruction`. For example, `Turn01_Decision_InfrastructureProject` offers investment first (sets the investment flag and decreases government budget by 1), then rejection (sets rejection and `GameCondition` flags).

Conditional-instruction ancillary records use `ConditionalInstructionProperties.ConditionalInstructions` with `Condition` and `Instruction`, together with scheduling fields including `CheckOnTurnNo`, `CheckOnStepNo`, `CheckPerTurn`, `CheckPerStep`, `CheckPerStoryFragment`, `Priority`, and `IsOneTime`. News uses `NewsProperties.Title`, `Description`, `TurnNo`, `Newspaper`, and `IsEnabledVariable`. Neither should be turned into fake decision options in the primary campaign tree.

## Representative verification

- Conversation 6: `Sordland/Turn01/Start_Inauguration`, 134 entries.
- Conversation 9: `Sordland/Turn01/Personal_Ball`, 241 entries.
- Conversation 1: `Sordland/Turn01/EnT_EconomicOverview`, 352 entries.
- Entry `1:3`: Narrator, English text beginning “Symon Holl, Gus Manger and Lileas Graf...”, exact outgoing destination `1:246`.
- Entry `1:348`: explicit `End();`, exact outgoing destination `1:2`.
- A complete loader smoke run with the local Liberica Full 25 JDK produced the counts above, resolved every link, and reported the known WIP turn conflict in approximately 1.4 seconds on the supplied machine.
