# Farming Navigator

Farming Navigator is a RuneLite Plugin Hub plugin designed to make Farming runs easier, faster, and more efficient.

## Planned Features

- Account-aware farming patch availability
- Farming level requirements
- Quest and diary requirement detection
- Farming patch timers
- Ready, growing, diseased, and dead patch states
- Herb runs
- Tree runs
- Fruit tree runs
- Hardwood tree runs
- Special farming patches
- Optimal farming route generation
- Best and second-best transportation methods
- Maximum XP per run optimization
- Maximum active XP per hour optimization
- XP per day planning
- Profit-focused routes
- GP-efficient routes
- Ironman-friendly routes
- Farmer protection payment tracking
- Dynamic farming run loadouts
- Inventory item highlighting
- Farmer NPC highlighting
- Farming patch highlighting
- Tile and destination markers
- Guided Run mode
- Timed route optimization

## Current Development Stage

The current build is an internally tested herb-run assistant with tree observations and preparation. The full roadmap above is not complete. All live client behavior is **UNVERIFIED IN GAME**.

- Account, quest, diary, claimed reward, spellbook, skill and configured POH requirements are checked for supported herb transports. Routes distinguish ready, banked, missing, locked, unknown and configuration-required states.
- A bank snapshot is trusted only after the bank has been opened in the current session. Unsupported transport semantics remain explicit instead of being guessed.
- Ten herb patches have region-validated observations for empty, weeds, growing, harvestable, diseased, dead and unknown states. Observations wait for settled game ticks and skip modals and instances.
- Last observations survive restart through RuneLite's character/world-mode profile configuration. Original timestamps are preserved; corrupt, future and over-30-day saved records are ignored.
- Harvest windows assume continued healthy growth. They do not prove readiness or predict disease. Normal and seasonal growth rates are kept distinct.
- Suggested herb circuits support 15 crops, selected patches, compost choice, farming supplies, shared rune/jewellery budgets and single-use teleport quantities. Every recommended POH visit reserves a house tablet or spell cast according to the selected entry-supply setting; house casts share the direct-teleport rune budget. Default transport ranking prioritizes ready routes; an optional preference favors verified reusable methods. Stop order is deterministic, not optimized by travel time.
- Guided Run provides preparation, travel, inspection, raking, harvest, clearing, compost confirmation, planting, curing and waiting steps. Fresh nearby patch observations can advance proven transitions. Manual completion and skipped stops remain separate from observed completion. Elapsed minutes include preparation and waiting and freeze at completion.
- Context-sensitive inventory/equipment and bank highlights, farming-area map markers, cached patch-object highlights and tool leprechaun guidance assist the current step. A full inventory pauses harvesting guidance until a slot is freed; it does not claim that noting occurred.
- Optional observed-state alerts and conditional estimated-ready alerts use RuneLite notification settings. Both default to off, avoid repeated alerts and suppress restored-history login bursts.
- The selected-patch overview separates historical conditions from possible readiness estimates. Cure Plant and Fertile Soil share the full-run rune pool with travel. Bottomless bucket supplies reserve one filled bucket and require manual type/charge checks. Ultracompost Fertile Soil reserves volcanic ash and uses an explicitly manual tome-unlock declaration. Selected infinite-rune equipment and required banked pouches are included in guidance; carried/banked battlestaves require sufficient unboosted Attack.

- Tree observations and preparation cover 25 patch identities and 22 crops across regular, fruit, hardwood, calquat, celastrus, redwood and crystal trees, with settled region checks, profile persistence and conditional health-check windows. Health-checked fruit trees do not imply fruit is available. Celastrus bark availability and harvested bark are separate states. Special-tree guidance includes verified farmer payments where applicable; mature redwoods retain their tree without reserving replacement materials. Crystal planning checks Song of the Elves, skips farmer protection and verifies Prifddinas crystal travel. Special-tree scene highlights use exact object bases and known gardeners.
- Tree preparation selects a sapling and matching patches, checks supported access requirements, excludes early growing trees and budgets potential replants and farmer payments. Noted/unnoted payments combine; planting requires unnoted finished saplings and baskets must be full. New hardwood payment data and three newer patch unlocks remain explicitly unverified.
- Tree destinations have an initial verified spell/jewellery catalogue, spirit-tree connections, Lletya crystals and Digsite pendant/barge travel, ranked recommendations and fallbacks, and a combined rune/charge budget. Adjacent Fossil Island patches share one arrival teleport. Unsupported legs require manual travel. Herb and tree budgets describe separate runs; combining the runs is not budgeted.
- Guided tree runs use fresh nearby observations for health checks, raking, clearing, planting and pruning. Keeping a healthy tree or removing it is an explicit choice. Farmer protection is confirmed manually. Manual completions, skips and protection confirmations are counted separately, and elapsed time freezes at completion. Starting herb or tree guidance replaces the other checklist; logout, hopping and profile changes clear both.
- Tree guidance uses the same inventory/equipment/bank overlays and destination markers as herb guidance, with step-specific sapling, payment, pruning and travel targets. Overlay toggles and cleanup apply to both run types.
- Tree patch objects and supported farmers use spawn/despawn caches. Scene highlights require matching patch region, transmit metadata and farming-area location. Farmer steps select known patch-specific gardeners; the three individual Fossil squirrel assignments remain suppressed pending verification.
- Session run statistics record Farming XP gained and elapsed XP/hour for herb/tree guidance, including preparation, waiting and other Farming actions during the checklist. Completed/stopped totals freeze; missing or decreasing XP invalidates the statistic. These are recorded account XP deltas, not predicted crop XP or precise patch attribution.
- Tree state and estimate alerts are separate opt-ins, default off. They follow selected patches matching the chosen tree family, suppress restored-history login bursts and repeated conditions, and keep estimated health-check readiness conditional.
- Expanded preparation cards show potential farming-materials values from RuneLite's existing cached prices. Unknown quotes remain unavailable; known subtotals include owned materials and exclude travel, spell runes, tools, removal fees and harvest value. Bottomless use costs and unverified protection payments stay excluded explicitly. This is a limited materials estimate, not profit or a complete run-cost model.
- The contract dashboard reads current guild-assignment and completion flags, recognizes 51 assignment IDs, and warns when the selected guild herb/tree crop conflicts with the assignment. An explicit preparation button selects supported herb, regular/fruit tree, celastrus or redwood assignments and their Guild patch, with a fresh-assignment check and active-guidance guard. It does not infer harvest completion, predict rewards or claim seed-pack ownership. Unsupported contract families remain explicit.

Configure crop, patches, compost, transport preference, POH entry supplies, overlays and alerts in the plugin settings. Configure the house and installed features in the sidebar's POH editor. Changing herb run setup cancels the active herb checklist.

## Current Limits

Other crop families, comprehensive tree transport and farmer coverage, automatic protection-purchase tracking, contract planning/reward analysis, economic/XP optimization, measured travel-time routing and comprehensive run analytics remain planned. Tool leprechaun storage, bottomless contents/charges, the tome unlock and compost application are not automatically verified. POH entry budgeting reserves a single chosen method for all house visits; mixed tablet/spell allocation is not optimized. Some catalogue transports remain unwired pending reliable unlock/charge evidence. Guided runs and elapsed time do not persist across logout or world hopping.

## Automated Validation

Use Java 11 and the Gradle wrapper:

```powershell
.\gradlew.bat clean build
.\gradlew.bat test
```

Tests use synthetic account/storage/game-state data and offline Swing rendering. They do not start a game client and cannot establish live RuneScape correctness. See [NEXT_SESSION_HANDOFF.md](NEXT_SESSION_HANDOFF.md) for the current checkpoint, research sources and suggested next milestones.

## Safety and RuneLite Compliance

Farming Navigator is intended to provide information, overlays, highlighting, routing, and recommendations.

It will not automate player input, click objects, click NPCs, move the player, or otherwise perform gameplay actions on behalf of the user.
