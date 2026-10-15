# Farming Navigator development checkpoint

Updated 2026-10-15. Repository and current Git history take precedence over this note.

## Session boundaries

- Work directly in `C:\Users\Admin\IdeaProjects\farming-navigator`, branch `master`, remote `https://github.com/phrenzia/farming-navigator`.
- User authorized focused source changes, automated builds/tests, commits and normal pushes to master. Do not rewrite history or discard unfamiliar changes.
- **Do not launch RuneLite, run the Gradle run task, log into RuneScape or interact with the game.** Validation is automated/internal only. Every live behavior remains **UNVERIFIED IN GAME**.
- Latest usage instruction: stop work when the remaining five-hour allowance falls below 10%. Check the account usage tool between milestones. Do not purchase credits or enable paid overage. Leave free resets unused for this run; earlier reset permission was superseded.
- Follow root `AGENTS.md`; the user's explicit automated-only restriction overrides its live-test launch procedure. Java 11; gameval constants; no reflection/native tricks/input automation; no blocking client-thread IO or repeated whole-scene scans.

## Implemented this session

Tree alerts checkpoint: 1,825 tests pass after clean build/test. New opt-in `treeObservedAlerts` and `treeEstimatedAlerts` default false, use native RuneLite notifier settings, and filter selected patches to the current tree run crop family. Restored-history baselines, fresh-state checks, deduplication and conditional upper-window estimates follow the herb pattern. Tree setup cancellation now lists only the four setup keys; changing alert/display preferences does not cancel guidance. No fruit-availability claims from health-checked trees.

Recorded statistics checkpoint: 1,816 tests pass after clean build/test; offline statistics card inspected. `analytics.RunMetricsTracker` samples the current Farming skill experience through AccountStateService while a herb/tree checklist is active. Shared XP deltas and elapsed XP/hour include all Farming actions, preparation and waiting, with explicit UI attribution limits. Missing baseline, unavailable/decreasing XP invalidates totals; duplicate/out-of-order samples do not add XP. Finish freezes XP/time and distinguishes completed from stopped. New run resets; account/world/profile/shutdown boundaries clear metrics. No persisted history, GP accounting, projected XP or exact patch attribution yet.

Tree scene checkpoint: 1,804 tests pass after clean build/test. `TreeSceneTracker` caches spawn/despawn objects and known farmers, with one nearby-area initialization on startup/loading and cleanup on all lifecycle boundaries. `TreeSceneTargetPolicy` requires region, anchor, transmit channel and tree family; canopy objects are suppressed. Patch-specific gardener IDs distinguish guild tree/fruit farmers and other supported locations. Individual Fossil squirrel assignments and three unverified newer patches have no farmer highlights. No per-tick/per-frame scene scans. All scene behavior remains UNVERIFIED IN GAME.

Tree item/map checkpoint: 1,796 tests pass after clean build and test. `GuidedRunTargets.fromTree` shares item renderers with herbs, keeps noted saplings out of planting targets, includes both farmer-payment forms, handles crystal/pendant/spirit-route items and selected rune equipment, and marks the current tree farming area during travel. Controller uses a generic farming tooltip and removes only its own marker. Tree scene/farmer targets remain the next step.

Guided tree checkpoint: 1,791 tests pass after clean build/test; the narrow manual-payment card was rendered and inspected offline. New `guided.tree` classes keep a fixed immutable stop plan, refresh transport choices, and gate evidence by timestamp, patch family, plane and a 12-tile farming-area anchor. Actions distinguish health check, keep/remove choice, stump/dead clearing, raking, planting, pruning, waiting and manually confirmed protection. Wrong/unknown fresh state returns to inspection; time passing alone never proves readiness. Manual stop completion/skipping do not claim planting or payment. Completed elapsed time freezes. Plugin wiring makes herb/tree sessions mutually exclusive and clears both plus cached plans on logout/hop/profile/shutdown. Tree settings cancel the tree checklist. **No tree overlays yet.** Tree patch anchors derive from pinned RuneLite FarmingPatchLocation; copyright retained. Fossil anchors represent the shared farming area, not individual clickable objects.

Latest tree checkpoint (1,761 tests; clean build and test passed; offline tree preparation image inspected):

Tree transport follow-up: 1,768 tests pass after clean build and test. `TransportVerificationService.evaluateAll` now accepts any patch-key map in its five-argument overload; the older four-argument herb API remains. `RunTravelStop` makes shared rune/charge budgets reusable across herb and tree stops. Eight tree destinations support initial verified spells/jewellery, including newly defined Varrock/Lumbridge spells. Tree config has its own transport preference. No live action or optimal-time claim. Before-bank transport verification still returns no recommendations. Tree panel displays all shared checks and manual-leg notes; herb/tree plans are explicitly separate budgets.

Additional tree travel checkpoint: 1,775 tests pass after clean build and test. Spirit routes use a charged wealth teleport to the Exchange then the spirit tree; finished Tree Gnome Village is required, but incoming Stronghold travel does not require The Grand Tree. Lletya supports five finite crystal variants and eternal crystal (empty/enhanced seeds excluded), gated by started Mourning's End I. Fossil routes use the pendant's **base Digsite destination** plus the Bone Voyage barge, without assuming the separate machine/mushtree unlock. A charged untradeable pendant supplies possession evidence for its learned enchantment. Adjacent selected Fossil patches become local walking legs after the first stop, so only one arrival teleport is budgeted. Source: [spirit destinations](https://oldschool.runescape.wiki/w/Spirit_tree_protection), [Digsite pendant](https://oldschool.runescape.wiki/w/Digsite_pendant), [barge](https://oldschool.runescape.wiki/w/Canal_barge), [crystal](https://oldschool.runescape.wiki/w/Teleport_srystal). No live validation.

- `1c2d6d6`: regular/fruit/hardwood state decoder, 780 pinned-source fixture cases and 18 crop growth definitions.
- `6b32d0b`: 19 tree patch identities; three settled ticks, modal/instance guards and precise transmit-region boundaries; profile/world-mode persistence using shared V1 record codec; conditional growth windows and collapsible observations.
- `177ad7f`: equivalent supply forms sum safely with deduplication and overflow saturation; noted/unnoted farmer payments can combine. Ordinary saplings still accept only their planting form.
- Tree preparation now supports 18 finished sapling choices, selected matching patches, boosted planting/guild access checks, started Mourning's End I, finished Bone Voyage/Ribbiting Tale, conditional growing exclusions and pruning supplies instead of replacement for observed disease. Full-basket payments for 15 established crops are reserved per potential replant. Three new hardwood payments remain DATA REQUIRED.
- Auburnvale, Kastori and Anglers' Retreat entry requirements remain unverified/excluded. Tree travel and removal fees are not budgeted. No tree guided session yet; healthy-tree removal stays optional. Fruit health-checked never promises six fruit.
- Tree sources: RuneLite SHA `e98399f931c60bfa8e42f924da850f508ec8e00d` FarmingWorld, PatchImplementation, Produce and FarmingAction. Relevant BSD notices retained. Wiki [tree guide](https://oldschool.runescape.wiki/w/Tree_farming_guide), [contract payment tables](https://oldschool.runescape.wiki/w/Contract_farming), [Lletya access](https://oldschool.runescape.wiki/w/Fruit_tree_patch/Patches). New hardwood farmer payments need reliable research before filling them in.
- Oak timing follows RuneLite Produce/states; older Wiki training tables disagree about total cycles. Keep conditional wording and verify in game only when the user authorizes their own manual testing. No live validation performed.

Continuation after the five-hour window refreshed (user repeated the same below-10%-remaining stop rule):

- `fc9f1d1`: selectable Cure Plant, Lunar/quest/boosted Magic eligibility, farming runes combined with all travel costs, matching guided instructions/targets.
- `f40c8bc`: one filled bottomless bucket instead of per-patch buckets. Type and charges remain DATA REQUIRED/manual because an arbitrary bucket's item ID does not establish them.
- `3c5193d`: Fertile Soil super/ultra selections; 83 Magic, Lunar Diplomacy and Lunar book; all casts pooled with travel/cure runes. Ultra reserves two volcanic ash per potential replant and requires an explicitly manual ash-covered-tome declaration. Super mode warns that an upgraded spell may consume carried ash which is not reserved in that mode.
- `baf687f`: selected/unlocked patch overview with historical conditions, possible readiness estimates and collapsible priority details; offline layout verified.
- `8681319`: existing infinite-rune equipment IDs migrated to gameval constants.
- `5e0aa04`: structured rune preparation choices, banked staff/pouch candidates and context-sensitive selected-staff highlights. Do not parse rendered requirement descriptions to derive item targets.
- Latest feature commit verifies unboosted Attack before suggesting carried/banked staff equipment changes. AccountState now carries real Attack; compatibility constructors use -1 (unknown). Already-equipped sources remain valid evidence. Synthetic helpers may explicitly supply Attack with `withRealAttackLevel`.

All continuation changes passed clean build and full tests (936 tests at the Attack-check checkpoint). RuneLite remains unlaunched. Bottomless contents, compost application and tome unlock are not automatically verified.

Commits through this checkpoint are individually pushed to master:

| Commit | Change |
| --- | --- |
| `7dbbd38` | Kandarin headgear 3 daily Sherlock route |
| `f727b54` | Explorer ring 2 daily Cabbage-port route |
| `065a322` | Ardougne and Fenkenstrain direct spell routes |
| `b2ac071` | Charged Xeric talisman Glade route |
| `24a274c` | Verified herb state decoder and 256-value golden fixture |
| `3cc6440` | Region-safe, settled-tick observations and patch-card display |
| `58fcdbb` | Conditional normal/seasonal herb harvest windows |
| `70a3949` | Suggested herb circuits, crop/patch/compost settings, quantity-aware farming supplies |
| `26be3e3` | Shared direct-spell rune and jewellery-charge budgets |
| `4751a8b` | Observation-driven guided checklist |
| `708531c` | Inventory/equipment/bank highlights and farming-area map markers |
| `d2becd9` | Full-inventory noting guidance and cached patch/leprechaun scene targets |
| `afbbcce` | Character/world-mode profile persistence with bounded record validation |
| `95a8cd9` | Opt-in observed and conditional estimated herb alerts |
| `4dc475b` | Run-wide single-use tablet/basalt/scroll supply checks |
| `1a83e15` | Reusable transport preference shared by suggested and guided runs |
| `8409cc5` | Aggregate POH entry tablet/cast budgets and matching highlights |
| `74c0598` | Guided elapsed time including preparation/waiting, frozen on completion |
| `fc75c80` | Shared rune/jewellery bank candidates highlighted during preparation |

Reusable preference preserves the default ready-before-bank catalogue order unless selected. Reusable mode ranks a conservative verified-unlimited subset first, including banked items; POH entries, charged items and limited daily uses are outside that subset. POH entry supplies are explicitly selected as tablets (default) or spells. The resource budget reserves one per recommended visit; house casts share runes with direct teleports and retain the existing Magic/spellbook/house requirement check. Mixed entry allocation is not optimized. Existing transport-card readiness still accepts either entry method, so inspect the selected run-wide supply budget as well.

## Architecture and behavior

- `farming/state`: decoder, region mapping, observation sampler/store/service, growth timer, profile persistence and alert tracker. The store exposes cached immutable snapshots. Profiles use RuneLite `ConfigManager` RS-profile keys and versioned per-patch records, not custom disk files. Saved records keep raw value, timestamp and growth rate; future/corrupt/over-30-day records are ignored.
- `routing/run`: herb crop/compost definitions, run planner, immutable plan/stops and aggregate transport resources. This includes selected house-entry budgeting and a shared reusable transport selector. Fixed patch order is not a shortest-path or measured-time optimizer.
- `loadout`: reusable quantity-aware checks against inventory, equipment and verified bank. Partial withdrawals and known shortfalls are distinct from unknown bank contents. Shared rune/jewellery pools also expose banked candidate items when the combined budget needs withdrawal, even if each route alone is ready. Candidates are not a minimal withdrawal solution and do not include all banked infinite-rune equipment or pouch alternatives.
- `guided`: checklist commands/state machine and cached semantic highlight targets. Compost confirmation remains manual; no compost state is invented. Arrival needs the correct area/floor and action progression needs fresh matching observations. Full inventory returns to harvesting after space is freed without claiming a leprechaun interaction was observed.
- `overlay`: lightweight item rendering, map-point lifecycle, cached herb objects/NPCs, event-driven scene tracker. Nearby initialization scans at most 25x25 tiles once per relevant map load/enable, not each tick/frame. No automated actions or clickable gameplay shortcuts.
- `ui`: compact plan, loadout, observation/timer text and guided panels. Offline screenshots are generated under ignored `build/verification` by tests.
- Existing bank gate still requires a bank verified in the current session. Missing daily counter entries mean unknown, while explicit zero means exhausted.
- Login/hop/connection loss cancels guided runs and clears live caches. Saved observations restore only into their matching RuneScape profile. Alert baseline suppresses historic login bursts; estimates remain conditional and do not complete guided steps. Guided elapsed time is session-only, includes preparation/waiting and freezes at completion. Negative wall-clock durations are clamped to zero; it is not an active XP/hour metric.

## Sources and unresolved semantics

Verified mechanics were researched against RuneLite source at `e98399f931c60bfa8e42f924da850f508ec8e00d`, and shortest-path transport data at `99735452c7c257d3fe5ae79970429cdce907d571`.

- [RuneLite farming tracker source](https://github.com/runelite/runelite/tree/e98399f931c60bfa8e42f924da850f508ec8e00d/runelite-client/src/main/java/net/runelite/client/plugins/timetracking/farming): HERB raw mapping, Produce growth cycles and region/transmit rules. Adapted BSD headers are retained. Scene anchors were taken from RuneLite farming patch locations and are area anchors, not guaranteed exact clickable tiles.
- [Shortest-path item transports](https://github.com/Skretzo/shortest-path/blob/99735452c7c257d3fe5ae79970429cdce907d571/src/main/resources/transports/teleportation_items.tsv): charged Xeric Glade, Explorer ring daily usage, and source for further route research.
- [RuneLite ConfigManager](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/config/ConfigManager.java): character/world-mode profile keys, explicit profile reads/writes and profile-change event.
- [RuneLite Notifier](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/Notifier.java): notification API respects the user's client notification settings.
- Limited Ardougne cloaks remain unwired: shortest-path warns only the counter's low bits transmit. Do not infer all remaining daily uses.
- Hunter cape guild teleport needs the Apatura dialogue unlock. Do not treat cape ownership alone as proof.
- Memoirs/book-of-the-dead route remains unwired. Shortest-path uses Hosidius reward varbit 6028 and shared charge varbit 6035, but page-added semantics and current capacity bounds require corroboration. Current search snippets conflict with old capacities; do not use outdated 40/60-charge assumptions.
- Local research copies are task scratch files under `C:\Users\Admin\Documents\Codex\2026-10-15\c\work`, not project inputs or committed resources. Prefer pinned online sources if unavailable.

## Suggested next milestones

1. Extend run supplies for verified leprechaun/barbarian unlocks and reliably observed compost application. Cure Plant, conservative bottomless supplies, Fertile Soil, shared staff/pouch preparation and Attack wield checks are now implemented. Research exact state before adding assumptions. Future mixed house tablet/spell allocation should retain the shared rune budget and explicit eligibility checks.
2. Validate more herb transports only after exact unlock/charge evidence is established. Preserve unknown/configuration-required states.
3. Extend the elapsed-time/completion summary into robust run analytics and a readiness-focused dashboard; separate additional UI components from the large existing panel. Do not turn session elapsed time into a claimed active XP/hour calculation without actual XP/time evidence.
4. Generalize patch/crop/timer models before adding trees, fruit trees, hardwoods and special patches. Build crop-specific protection and contract state from proven signals.
5. Add optimization objectives and economics only with explicit, reliable costs/timing/yield models. Current baseline ranks and reusable preference must not be labeled fastest or profit-optimal.

## Manual validation queue (for the user later)

All items below remain **UNVERIFIED IN GAME**. No game client was launched during development.

- Bank gate, account/mode switching, logout/hop restoration and prevention of cross-character observations.
- Each regional transmit boundary, modal settling, object transformation, inventory-full transition and leprechaun target.
- Guided arrival, harvest/clear/rake/cure/replant progression, manual compost confirmation and stale-data rejection.
- Overlay/map cleanup, settings toggles and narrow sidebar layout at the user's scaling settings.
- Daily charge counters, spellbook/rune pouch/infinite sources, bank withdrawals, shared budgets and usable fallback changes.
- Optional notification behavior, focus settings, deduplication, normal/seasonal estimates and no login bursts.

## Validation checkpoint

`gradlew.bat clean build` and `gradlew.bat test` passed after the shared-bank-highlight correction; 897 synthetic tests passed with zero failures/errors. The updated narrow guided panel was rendered and visually checked offline. Known build messages are the existing unchecked test-harness warning and Gradle 9 deprecation notice. Automated success does not prove live correctness. All feature commits above were pushed; check `git status` and `git rev-parse HEAD origin/master` before continuing.
