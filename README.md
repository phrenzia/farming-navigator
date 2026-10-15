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

The current build is an internally tested herb-run assistant. The full roadmap above is not complete. All live client behavior is **UNVERIFIED IN GAME**.

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

Configure crop, patches, compost, transport preference, POH entry supplies, overlays and alerts in the plugin settings. Configure the house and installed features in the sidebar's POH editor. Changing run setup cancels the active checklist.

## Current Limits

Tree, fruit-tree, hardwood and other crop families, contracts, protection payments, economic/XP optimization, measured travel-time routing and comprehensive run analytics remain planned. Tool leprechaun storage, bottomless contents/charges, the tome unlock and compost application are not automatically verified. POH entry budgeting reserves a single chosen method for all house visits; mixed tablet/spell allocation is not optimized. Some catalogue transports remain unwired pending reliable unlock/charge evidence. Guided runs and elapsed time do not persist across logout or world hopping.

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
