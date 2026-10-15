# Farming Navigator development checkpoint

Updated 2026-10-15. Repository and current Git history take precedence over this note.

## Session boundaries

- Work directly in `C:\Users\Admin\IdeaProjects\farming-navigator`, branch `master`, remote `https://github.com/phrenzia/farming-navigator`.
- User authorized focused source changes, automated builds/tests, commits and normal pushes to master. Do not rewrite history or discard unfamiliar changes.
- **Do not launch RuneLite, run the Gradle run task, log into RuneScape or interact with the game.** Validation is automated/internal only. Every live behavior remains **UNVERIFIED IN GAME**.
- Latest usage instruction: stop work when the remaining five-hour allowance falls below 10%. Check the account usage tool between milestones. Do not purchase credits or enable paid overage. Leave free resets unused for this run; earlier reset permission was superseded.
- Follow root `AGENTS.md`; the user's explicit automated-only restriction overrides its live-test launch procedure. Java 11; gameval constants; no reflection/native tricks/input automation; no blocking client-thread IO or repeated whole-scene scans.

## Implemented this session

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

Reusable preference preserves the default ready-before-bank catalogue order unless selected. Reusable mode ranks a conservative verified-unlimited subset first, including banked items; POH entries, charged items and limited daily uses are outside that subset. POH entry supplies are explicitly selected as tablets (default) or spells. The resource budget reserves one per recommended visit; house casts share runes with direct teleports and retain the existing Magic/spellbook/house requirement check. Mixed entry allocation is not optimized. Existing transport-card readiness still accepts either entry method, so inspect the selected run-wide supply budget as well.

## Architecture and behavior

- `farming/state`: decoder, region mapping, observation sampler/store/service, growth timer, profile persistence and alert tracker. The store exposes cached immutable snapshots. Profiles use RuneLite `ConfigManager` RS-profile keys and versioned per-patch records, not custom disk files. Saved records keep raw value, timestamp and growth rate; future/corrupt/over-30-day records are ignored.
- `routing/run`: herb crop/compost definitions, run planner, immutable plan/stops and aggregate transport resources. This includes selected house-entry budgeting and a shared reusable transport selector. Fixed patch order is not a shortest-path or measured-time optimizer.
- `loadout`: reusable quantity-aware checks against inventory, equipment and verified bank. Partial withdrawals and known shortfalls are distinct from unknown bank contents.
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

1. Extend run supplies for bottomless compost, Fertile Soil, alternative cures and verified leprechaun/barbarian unlocks. Research exact state before adding assumptions. POH budgeting is now implemented; future mixed tablet/spell allocation should retain the shared rune budget and explicit eligibility checks.
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

`gradlew.bat clean build` and `gradlew.bat test` passed after the final elapsed-time feature; 894 synthetic tests passed with zero failures/errors. The updated narrow guided panel was rendered and visually checked offline. Known build messages are the existing unchecked test-harness warning and Gradle 9 deprecation notice. Automated success does not prove live correctness. All feature commits above were pushed; check `git status` and `git rev-parse HEAD origin/master` before continuing.
