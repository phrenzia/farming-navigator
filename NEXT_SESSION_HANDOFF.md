# Farming Navigator: resume checkpoint

Updated 2026-10-15. Current repository and Git history win over documents.

## Safety and workflow

- Repository: `C:\Users\Admin\IdeaProjects\farming-navigator`; branch `master`; origin `https://github.com/phrenzia/farming-navigator`.
- Latest validated source checkpoint: `b95e5450753a4113d0063c9b54fa19edc25bae95`. This handoff is a following documentation-only commit. Check `git status`, `git log`, and `git rev-parse HEAD origin/master` on resume.
- User authorized direct edits, automated validation, normal commits and pushes. Never force-push, rewrite history or discard unfamiliar changes.
- **Never launch RuneLite, execute the Gradle run task, log into RuneScape or interact with the game.** This overrides AGENTS.md's live-test launch procedure. Everything remains **UNVERIFIED IN GAME**.
- Stop when the five-hour allowance falls below **10% remaining**. Check the account usage tool between milestones. No paid usage, upgrades or reset credits were consumed.
- Java 11 and gameval constants; no reflection/native/input automation. No blocking client-thread IO or per-tick/per-frame scene scans. Follow root AGENTS.md.
- Each source milestone: focused tests, `gradlew.bat clean build`, `gradlew.bat test`, inspect diff, explicitly stage, commit, push.

## Validation

- Latest source: **4,123 tests, zero failures/errors**. Clean build and test pass.
- Offline Swing renders inspected for tree, celastrus bark, bush/cactus observations, preparation and manual produce confirmation. Long garden button labels were shortened to fit the narrow sidebar.
- Existing build notices: unchecked test-harness operations and Gradle 9 deprecations. Automated checks do not prove live correctness.
- Completed milestones are committed/pushed. No game client was launched.

## Implemented architecture

- Existing herb observations, profile persistence, planning, guided steps, item/map/scene overlays, opt-in alerts, account/quest/diary/travel awareness, verified bank snapshots, charged transports, shared runes/pouches/infinite sources and POH configuration remain intact.
- Cure Plant and Fertile Soil share runes with travel. Bottomless supplies reserve one filled bucket with manual contents/charge checks. Ultra Fertile Soil uses a manual tome declaration and ash. Infinite-rune equipment checks real Attack for wielding.
- Tree system: **25 patches, 22 crops**, including calquat/celastrus/redwood/crystal. Decoder has **1,820 pinned-source fixture cases**. Three settled regional ticks; modal/instance/travel/gap guards; cached/throttled observations; profile-safe V1 persistence; conditional normal/Leagues growth windows.
- Special trees: three calquat patches, Guild celastrus/redwood, Prifddinas crystal. Guild G/K/L/I and Kastori A/B channels are separate. Celastrus bark available/harvested/stump are distinct. Mature fruit states do not promise fruit quantities.
- Tree preparation/guidance: finished saplings, boosted access/planting levels, exact known farmer payments, optional keep/remove, separate health check/clear/rake/plant/prune/manual protection. Mature redwoods reserve no replacement/payment; explicit removal costs 2,000 coins (not budgeted), then reprepare. Crystal requires finished Song of the Elves and never asks for farmer protection.
- Standard axe preparation checks eight bronze-to-dragon axes against boosted Woodcutting; unknown/drained skill stays DATA REQUIRED. Noted axes and other variants are explicitly unchecked. Attack and Woodcutting snapshot-copy methods preserve each other.
- Tree travel includes initial spells/jewellery, spirit routes, Lletya/Prifddinas crystals, Digsite pendant plus Bone Voyage barge, Avium AJP, and Tai Bwo Wannai loose scroll/CKR. Scroll requires completed Trio; the calquat patch itself does not. Adjacent Fossil patches share one arrival teleport. Master scroll book contents are unchecked.
- Tree scene policy uses family/region/channel/anchor checks, suppresses canopies/upper redwood sections, and identifies verified gardeners. Notifications now name each special-tree family and avoid duplicated 'tree tree'.
- New `farming.garden`: **seven bush/cactus patches, eight crops**, **520 exhaustive fixture cases**. Covers Rimmington, Etceteria, Champions' Guild, Ardougne and Guild bushes; Al Kharid/Guild cacti. Region channels, persistence and conditional first-growth timers match tree safeguards. Mature states do not predict produce quantity/regrowth.
- Garden preparation defaults **KEEP_MATURE**; explicit REPLACE_MATURE reserves one seed and exact protection per possible new plant. Mature/unchecked kept plants and disease-only stops reserve no replacements. Bush disease uses secateurs; cactus disease uses plant cure. Guild entry is boosted 45. Seed dibber/compost/leprechaun unlocks are not inferred.
- Garden travel: Guild necklace/CIR; Al Kharid glory; Champions' bracelet/Varrock spell (outside guild); Rimmington Falador spell; Ardougne spell/DJP. Shared rune/charge/bank budgets and preferences apply.
- Garden guidance: fresh nearby evidence; manual produce confirmation, explicit replacement choice, clear/rake/plant/manual protection/prune/cure/wait. Wrong/unknown/stale/future/far/plane/family evidence cannot claim planting/cure. Unreserved plants that become empty finish without requesting unbudgeted planting.
- All **four** run types are mutually exclusive, clear on logout/hop/profile/shutdown, and share recorded actual Farming XP/elapsed statistics. XP includes unrelated Farming actions and preparation/waiting; no exact patch attribution or persisted history.
- Garden item/map/object/gardener highlights reuse shared renderers. Scene caches use spawn/despawn/NPC-change events and one nearby initialization, never tick/frame scans. Guild B/F cannot cross-match.
- Cached-price material subtotals cover reserved seeds/saplings, known protection, normal compost/cure/ash where supported. Garden keeps do not invent replacement costs. Unknown prices stay unknown; tools/travel/runes/removal/harvest are excluded. Not profit/total-run/Ironman acquisition cost.
- Contracts: current TYPE/COMPLETE flags, 51 IDs, crop conflicts and explicit setup for herb/tree/celastrus/redwood/bush/cactus. Handler rechecks assignment and refuses setup during any active checklist. Garden contract setup selects its Guild plot and REPLACE_MATURE, clearly explained; clearing remains a later manual choice. No reward/seed-pack ownership prediction.

- Bush/cactus observed/estimated alerts are now implemented as separate default-off opt-ins, filtered to selected patches in the chosen family, with session deduplication and login-burst suppression.
- `farming.field`: 26 allotment/flower plots and 14 crops, 520 exhaustive pinned decoder cases. Alternate growth ranges and snape grass split ranges are covered. Scarecrows have a distinct non-crop condition; harvest stages do not imply yield.
- Field observations use three settled ticks, exact regional channels/bounds (including Catherby/Falador overlaps), immutable throttled caches and profile-safe `fieldObservationV1_` persistence with the shared codec. Lifecycle resets match the other families. Sidebar history is collapsed by default.
- Field preparation: three seeds per allotment, one per flower, exact known payments, plant cure for disease, one filled unnoted bucket per possible new planting. Default KEEP_MATURE_FLOWERS protects mature flowers from implied harvesting; scarecrows are excluded. Guild 45 boost, Prifddinas SOTE, Morytania Priest in Peril and Ortus Children of the Sun are checked. Harmony permits verified completed Great Brain Robbery access, needs no herb diary reward and has no farmer; partial quest stage remains DATA REQUIRED. Kastori remains excluded.
- `LoadoutService.evaluateInOrder` is an opt-in ordered reservation path. Field plans list strict unnoted compost application before flexible noted/unnoted potato payments, preventing reuse of carried/banked buckets. It is not a general optimal matching solver; old independent evaluation is unchanged. Existing planner/snapshot constructors retain no-compost compatibility; production config `fieldRunCompost` defaults ULTRACOMPOST.
- Field travel reuses verified shared-area herb arrivals, beginner Guild routes and Prifddinas crystals. POH/spell metadata is preserved; adjacent plots in one area reserve one arrival and then local walking. Different areas still share rune/charge budgets. Map marker labels update when north/south share an anchor.
- Field guidance: fresh nearby <=30-second observations; harvest-to-empty, clear/rake, manually confirmed compost, matching planting, same-crop cure, manual protection, keep-flower confirmation and scarecrow caution. Compost confirmation persists for the stop across unknown evidence and resets between stops. All four run sessions are mutually exclusive, lifecycle-safe and included in actual XP/elapsed metrics. Field setup keys cancel only field guidance.
- Field inventory/bank/map/object/gardener highlights are implemented. Object bases must match family/channel/area; exact farmer assignments cover eight regions and never flowers/Harmony. Scene caches use spawn/despawn/NPC changes and one nearby initialization. WAIT/PREPARE/SCARECROW suppress action outlines. All remains UNVERIFIED IN GAME.
- All 51 recognized contract assignments now support explicit preparation. Allotments select only Guild NORTH; flowers select Guild FLOWER and HARVEST_ALL with a clear mature-flower/protection notice. Active guidance and stale assignment guards remain. Same-family field crop conflicts are shown.
- Field material values cover actually reserved seeds, payments, cure and application compost. Kept flowers invent no costs; potato application/payment are both counted, not confused. Unknown prices stay unknown.

## Next work and known gaps

1. Extend annual field support to hops using verified metadata/byte ranges, then remaining special patches. Hops are not implemented yet. Field observed/estimated alerts are now separate default-off opt-ins with selected-family filtering; scarecrows do not alert readiness.
2. Extend field/garden care to verified bottomless/spell/watering options, and improve readiness/dashboard/history. Keep manual care confirmation separate from observed evidence; avoid reusing resources across payments/application/travel.
3. Verify Etceteria entry; Auburnvale/Kastori/Anglers' Retreat/Great Conch entry; three newer hardwood payments; individual Fossil squirrel assignments. Current unknowns remain excluded or suppressed. FROG_QUEST_PATCH_UNLOCKED semantics beyond completed Ribbiting Tale remain unresolved.
4. Extend axe variants, leprechaun storage/barbarian unlocks and observed compost only with exact evidence. Existing variants/tome/bottomless checks are deliberately conservative.
5. Broader transports, saved run history, readiness dashboard, contract rewards, reliable yield/XP/economic objectives and measured routing remain planned. Catalogue ranks are not fastest/profit-optimal claims. Mixed POH tablet/spell allocation is not optimized.

## Sources and recovery

- RuneLite pinned SHA: `e98399f931c60bfa8e42f924da850f508ec8e00d`; FarmingWorld, PatchImplementation, Produce, FarmingAction, FarmingPatchLocation and gamevals. Adapted BSD notices retained.
- Shortest-path pinned SHA: `99735452c7c257d3fe5ae79970429cdce907d571`; jewellery, scroll and fairy-ring destinations.
- [Bush seed table](https://oldschool.runescape.wiki/w/Bush_patch/Seeds), [contract rules/payments](https://oldschool.runescape.wiki/w/Contract_farming), [tree guide](https://oldschool.runescape.wiki/w/Tree_farming_guide), [Guild tiers](https://oldschool.runescape.wiki/w/Farming_guild), [Alexandra removal](https://oldschool.runescape.wiki/w/Transcript:Alexandra).
- Scratch includes field_foundation.py, field_fixture.py, field_observations.py, field_planner.py, field_travel.py, field_guidance.py, field_guidance_integration.py, field_targets.py, field_scene.py, field_contracts.py, field_alerts.py, field_values.py and field_compost_* scripts. These are development scratch, not runtime code.
- Scratch research, generators and source snapshots: `C:\Users\Admin\Documents\Codex\2026-10-15\c\work`. Includes ObjectID1.java, the inherited generated second half of ObjectID, and independent tree/garden fixture generators. Scratch is not committed.
- Detailed earlier milestones/research survive in [the preceding handoff history](https://github.com/phrenzia/farming-navigator/blob/42fca74c4694d3aa49e538851ce008472240f753/NEXT_SESSION_HANDOFF.md). Read relevant source before changes; do not reconstruct already completed systems.
- User attachments: Downloads/Farming_Navigator_Desktop_Agent_Handoff.pdf and `.codex/attachments/9a5c76f1-38fe-42f0-88d9-de947ab2287f/Pasted text.txt`; already read during this continuing chat. Pasted text is the request; PDF is supporting context.

All live patch transforms, routes, game-state semantics, callbacks, overlays and notifications still need the user's eventual manual validation. Do not offer or perform a client launch during this session.
