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
- Bush/cactus observations and preparation cover seven patches and eight crops, with settled region channels, profile persistence and conditional first health-check windows. Preparation keeps mature plants by default; explicit replacement reserves one seed and the verified farmer payment per possible new plant. Diseased bushes reserve secateurs and diseased cacti reserve plant cure. Etceteria access remains unverified/excluded. Guidance uses fresh observations and manual produce/protection confirmation, with verified travel, shared supplies, item/map/object/gardener highlights and actual XP statistics. Mature plants do not imply a known produce quantity.
- Tree preparation selects a sapling and matching patches, checks supported access requirements, excludes early growing trees and budgets potential replants and farmer payments. Noted/unnoted payments combine; planting requires unnoted finished saplings and baskets must be full. New hardwood payment data and three newer patch unlocks remain explicitly unverified.
- Tree destinations have an initial verified spell/jewellery catalogue, spirit-tree connections, Lletya crystals and Digsite pendant/barge travel, ranked recommendations and fallbacks, and a combined rune/charge budget. Adjacent Fossil Island patches share one arrival teleport. Unsupported legs require manual travel. Each run type has a separate supply budget; combining the runs is not budgeted.
- Guided tree runs use fresh nearby observations for health checks, raking, clearing, planting and pruning. Keeping a healthy tree or removing it is an explicit choice. Farmer protection is confirmed manually. Manual completions, skips and protection confirmations are counted separately, and elapsed time freezes at completion. Starting herb or tree guidance replaces the other checklist; logout, hopping and profile changes clear both.
- Tree guidance uses the same inventory/equipment/bank overlays and destination markers as herb guidance, with step-specific sapling, payment, pruning and travel targets. Overlay toggles and cleanup apply to all three run types.
- Tree patch objects and supported farmers use spawn/despawn caches. Scene highlights require matching patch region, transmit metadata and farming-area location. Farmer steps select known patch-specific gardeners; the three individual Fossil squirrel assignments remain suppressed pending verification.
- Session run statistics record Farming XP gained and elapsed XP/hour for herb/tree/bush/cactus/allotment/flower guidance, including preparation, waiting and other Farming actions during the checklist. Completed/stopped totals freeze; missing or decreasing XP invalidates the statistic. These are recorded account XP deltas, not predicted crop XP or precise patch attribution.
- Recent run history saves the last 20 completed or explicitly stopped checklists locally per RuneLite character/mode profile, including switches to another run. It retains unknown XP, deduplicates refreshes and supports clearing the current profile. Recording can be disabled; logout/hop/shutdown interruptions are not saved. History includes preparation, waiting and unrelated Farming XP, not crop attribution.
- Tree state and estimate alerts are separate opt-ins, default off. They follow selected patches matching the chosen tree family, suppress restored-history login bursts and repeated conditions, and keep estimated health-check readiness conditional.
- Bush/cactus state and first-growth estimate alerts are separate opt-ins, default off. They follow selected patches in the chosen crop family, suppress restored-history bursts and repeats, and never predict produce quantity or regrowth.
- Allotment/flower observed and estimated harvest alerts are separate opt-ins, default off, with selected-family filtering and session deduplication. Scarecrows do not trigger harvest readiness; produce quantities remain unknown.
- Allotment/flower history covers 26 plots and fourteen crops, with distinct Guild channels, profile-safe saved observations and conditional harvest windows. Scarecrows are shown separately; harvest stages do not predict yield. Field preparation reserves three seeds per allotment, exact known farmer payments and one seed per flower, while keeping mature flowers by default and preserving scarecrows. Field travel reuses verified shared-area arrivals, preserves spell/POH metadata and budgets one arrival per adjacent group of plots. Field guidance uses fresh nearby plot evidence for harvest-to-empty, clearing, raking, planting and curing; kept flowers and protection payments require explicit confirmation. Field inventory/bank and map highlights show step supplies and shared travel resources; neighboring plots update the marker label without reserving another teleport. Field object/gardener outlines use event caches and require matching family, regional channel and farming area. Scarecrow steps suppress action outlines. Field compost reserves one unnoted filled bucket per possible planting, or one filled bottomless bucket with a manually declared type and use check, separately from farmer payments and with manual application confirmation. Potato payment and application buckets cannot reuse the same stock. Bottomless use value is excluded explicitly. Field Cure Plant and Fertile Soil share one rune budget with travel and POH entry. Ultracompost needs two ash per possible cast and an explicit tome declaration; application stays manual. Underwater spell care remains excluded until verified. Optional post-planting watering budgets verified standard-can doses and advances on a fresh nearby watered state of the same crop, with manual confirmation as a fallback for allotments/flowers/hops. Only the current growth cycle is covered; refills and Gricoller charges are not assumed. A plot that becomes empty after a confirmed planting requires a new plan before a second seed is requested.
- Bush/cactus Cure Plant and Fertile Soil modes share travel runes, check Lunar access and manual tome declarations, reserve ash only for possible new plants, and keep manual compost confirmation distinct from observed healthy growth.
- Bush/cactus preparation reserves standard compost per possible planting or one filled bottomless bucket with manually checked contents/uses. Kept mature and disease-only stops reserve none. Application requires explicit confirmation; current compost state is not inferred, and bottomless use value is excluded.
- Seaweed, bittercap mushroom and belladonna observations extend field history to 36 plots and 27 crops with 1,560 pinned-source decoder fixtures. Underwater north/south channels remain distinct, while yield and harvest quantities are unknown. All three families support preparation and guidance: one seed/spore per planting, 200 numulites per protected seaweed patch, no mushroom/belladonna farmer payment, explicit gloves and underwater-equipment reminders, and verified CKS/Kharyrll and Draynor Manor spell arrivals, plus Digsite pendant/barge/northern-rowboat guidance for seaweed (shortcut unlock checked manually). Auburnvale remains excluded for unverified access.
- Optional tree compost supports buckets, manually checked bottomless contents, or Fertile Soil with shared travel runes; tree Cure Plant is an alternative to pruning. Disease-only stops and kept mature redwoods reserve no replacement compost. Crystal-tree composting stays excluded as unverified. Current compost/application and tome unlocks remain manual checks.
- Saved history can be filtered by run type or checklist end time (past 24 hours / 7 days / all stored). Summaries show completion counts, known XP, elapsed time and a weighted rate for timed known-XP entries. The last-20 cap, unknown XP, zero-duration exclusions and clear-all behavior remain explicit.
- Readiness details compare observed work and conditional future windows by run type across selected accessible crop families. Each type shows its next estimated patch and cumulative additional possibilities in 10/30/60 minutes. Individual preparation plans still determine crop, travel and supplies; these counts are not XP, profit or route rankings.
- Axe preparation recognizes standard bronze-to-dragon and 3rd age axes against current boosted Woodcutting; inventory use does not require the wielding Attack level. Noted and blessed undead-only axes do not qualify; remaining variants stay unchecked.
- Sidebar checklist commands are bound to the displayed run revision. A delayed click cannot confirm, skip, complete or cancel a newer step, stop or restarted run; rejected clicks refresh the sidebar.
- Optional combined preparation sums selected run types into one farming-supply and travel/spell/charge budget. Reusable tools can be shared; consumables cannot be counted twice, and distinct declared bottomless contents need separate buckets. Component route order and arrival legs remain explicit; use individual checklists for guidance. This worksheet does not optimize routes or predict inventory capacity/profit.
- A combined farming readiness card covers selected accessible patches across supported families, keeps health-check/bark/mature/scarecrow states distinct, and lists cumulative conditional readiness windows for 5/10/20/30/60 minutes. Estimates never become observed harvestability or promise yield/regrowth.
- A field overview groups selected accessible plots by historical problems, harvestability, growing/planting states and conditional possible readiness. Scarecrows remain separate, unknown/future/wrong-family evidence cannot become ready, and access/data exclusions remain explicit.
- Hops/fibre observation support adds five plots and ten pinned crops, including flax, hemp and cotton: field history now covers 31 plots and 24 crops with 780 decoder fixture cases. Seven traditional hops crops support preparation, care, guidance, highlights and material values through field runs. Four seeds are reserved per planting, except three for jute. Verified hops arrivals include Lumbridge/Camelot spells and ordinary CIQ/ALS fairy rings, with shared travel budgets. Flax/hemp/cotton remain observations only; Entrana restrictions and Aldarin entry remain unverified and excluded from preparation.
- Expanded preparation cards, including allotments/flowers, show potential farming-materials values from RuneLite's existing cached prices. Unknown quotes remain unavailable; known subtotals include owned materials and exclude travel, spell runes, tools, removal fees and harvest value. Bottomless use costs and unverified protection payments stay excluded explicitly. This is a limited materials estimate, not profit or a complete run-cost model.
- The contract dashboard reads current guild-assignment and completion flags, recognizes 51 assignment IDs, and warns when the selected guild herb/tree/bush/cactus/allotment/flower crop conflicts with the assignment. An explicit preparation button selects supported herb, regular/fruit tree, celastrus, redwood, bush, cactus, allotment or flower assignments and their Guild patch, with a fresh-assignment check and active-guidance guard. It does not infer harvest completion, predict rewards or claim seed-pack ownership. All 51 recognized assignments support preparation. Allotments select one Guild plot; flower contract setup explicitly enables mature-flower harvesting and explains its protection implications.

Configure crop, patches, compost, transport preference, POH entry supplies, overlays and alerts in the plugin settings. Configure the house and installed features in the sidebar's POH editor. Changing a run's setup cancels its checklist.

## Current Limits

Allotments, flowers, hops and remaining special crop families, comprehensive tree transport and farmer coverage, automatic protection-purchase tracking, unsupported contract planning/reward analysis, economic/XP optimization, measured travel-time routing and comprehensive run analytics remain planned. Tool leprechaun storage, bottomless contents/charges, the tome unlock and compost application are not automatically verified. POH entry budgeting reserves a single chosen method for all house visits; mixed tablet/spell allocation is not optimized. Some catalogue transports remain unwired pending reliable unlock/charge evidence. Guided runs and elapsed time do not persist across logout or world hopping.

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

Guided checklists preview the first and next planned destination, including an existing nearby-walk leg, and label the final stop. The preview follows checklist order and does not perform travel or advance a stop.

Transport preference can optionally repair a shared resource shortage by testing a bounded set of single-arrival substitutions. Farming spells, travel, POH entry and known charged/consumable pools stay in the same budget. The first covered alternative preserves patch order and existing nearby walks; otherwise the original suggestions and blockers remain. Bank withdrawals still need attention. Unmodelled daily/charge pools remain DATA REQUIRED in this mode. It does not search multi-leg combinations or optimize speed or profit. Active guidance keeps the selected alternative while it remains individually verified usable; changing resources may require a new preparation plan.

Whole-run travel budgets sum verified remaining Explorer's ring 2 Cabbage-port and Kandarin headgear 3 Sherlock uses across recommended arrivals, including combined preparation. Nearby walks and fallback alternatives do not spend uses. Unknown counters stay DATA REQUIRED; unlimited variants remain separate.

Vineyard observations add twelve independent East/West grape plots (four-bit transmit channels), bringing field history to 48 plots and 28 crops with 1,820 pinned-source decoder fixtures. Conditional growth windows reuse RuneLite metadata. Harvest stages do not predict quantity; terminal growing codes and fertiliser-empty states do not prove watering or readiness. Grape preparation, access gating, travel and scene guidance remain unverified and unavailable. Field observations now offer a family filter, retained during sidebar refreshes, with counts for the displayed family.

Great Conch coral history adds two independently sampled nurseries and three pinned crops (50 field plots, 31 crops, 2,080 decoder fixture cases). An observed Collect option is distinct from harvestability and growing; collection stages never generate a new growth window or quantity estimate. Nursery-only codes do not request raking. Entry, planting, pruning, protection, transport and scene guidance remain unverified and unavailable; historical observations are accessible through the Coral nurseries filter.

Anima history adds the Farming Guild M-channel patch and Attas/Iasor/Kronos, bringing field observations to 51 plots, 34 crops and 2,340 pinned-source fixture cases. Living, withering and dead stages are distinct. Withering never becomes an ordinary disease alert, and elapsed time never generates an anima harvest forecast. Effects, Kronos growth skips, expiry, entry, planting and gardener guidance remain unverified and uncalculated. Anima observations are available through the family filter.

Player-grown spirit-tree history covers Port Sarim, Hosidius, Etceteria, Brimhaven and the Farming Guild, bringing tree observations to 30 patches, 23 crops and 2,080 independent decoder cases. The observed check-health and mature travel-enabled stages remain distinct; mature history does not enable teleport routes or planting. Conditional growth windows follow pinned metadata. Entry, planting limits, protection and guided scene/travel support remain unverified and unavailable. Tree observations now provide a family filter that survives sidebar refreshes. Etceteria's area anchor is cross-checked against pinned shortest-path data because the pinned RuneLite location list has an inconsistent x coordinate.

Optional **Session planted-tree access** (off by default) opens a sidebar card for manually declaring currently usable Port Sarim, Hosidius, Brimhaven and Farming Guild trees. Applying selections enables extra arrivals via a charged ring of wealth to the Grand Exchange and its spirit tree. Tree Gnome Village, ring charges, verified bank stock and destination patch access remain checked separately; related arrivals share one wealth-charge budget and standard item/bank highlights. Declarations stay in memory for the current character/mode and clear on logout, world hop, connection loss, profile change or disabling; ordinary region loading preserves them. Changed declarations stop current guidance for review. Cached observations never declare access, and stale sidebar callbacks cannot cross profiles or overwrite newer declarations. Etceteria arrivals remain unwired while patch access is unverified. No spirit-tree planting or automatic unlock detection is added.

Hespori farming observations use the pinned cave transmit region and a distinct mature-patch state. Conditional maturity windows never establish combat/reward state, ordinary crop harvestability or entry access. This passive history entry has no planting checklist, travel option, scene guidance or alerts. All live behavior remains **UNVERIFIED IN GAME**.

Combined preparation can show the selected components' potential materials value using the existing cached quotes. Repeated consumables remain budgeted per use; tools, bottomless bucket purchase prices, travel and spell runes stay excluded. Unknown quotes remain explicit, and this subtotal is not profit or a shopping list. Recent history also offers a collapsible comparison by run type within the same filtered saved sample, including recorded XP and completed checklist times.

Combined preparation includes a collapsible arrival-sequence preview. It preserves component stop order, repeated arrivals and existing local walks, shows each leg's transport state, and keeps the shared withdrawal/resource checks visible as the authority for the full sequence. The preview never starts or advances guidance.

The optional “Repair shared budget (up to two arrivals)” preference tries the existing single-arrival search first, then at most 64 pairs using up to four distinct usable alternatives per stop. Every candidate keeps stop order and local walks and rechecks the full travel/POH/farming-spell budget. Unsuccessful search retains the original plan and blockers. Defaults and the existing one-arrival mode are unchanged; this bounded search does not claim global, time or profit optimality.

Field and bush/cactus harvest guidance pauses when the observed inventory is full near the current plot. Make space manually, then return with a free slot to resume the same harvest. Unknown slot data and other locations do not advance it. The pause clears action/item/map targets and invalidates stale sidebar commands; freeing space never establishes yield or harvest completion.

Completed Barbarian Planting is read from the exact named state (3), with partial and unexpected values kept conservative. Verified completion omits seed dibbers from herb, allotment, flower, hops and bush preparation and planting highlights. Cactus, spores, belladonna and other special families retain their tools until separately verified. Seed quantities, care, spades, pot smashing and seed-saving assumptions are unchanged.

Celastrus bark harvesting also pauses on a full inventory. Freeing space resumes the same harvest; fresh depleted-bark evidence is still required before offering tree removal. Paused bark guidance clears action targets and rejects stale checklist controls. Other tree phases are unaffected.

Saved run history can filter all outcomes, completed checklists only, or stopped checklists only, alongside the existing period and run-type filters. Totals and type comparisons use that exact filtered sample. Filtering happens after the global last-20 cap and never restores older hidden records; Clear history still removes all saved entries.
