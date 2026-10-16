package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.AccountStateService;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchService;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchObservationService;
import com.farmingnavigator.farming.state.HerbObservationPersistence;
import com.farmingnavigator.farming.state.HerbAlertTracker;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchObservation;
import com.farmingnavigator.farming.tree.TreePatchObservationService;
import com.farmingnavigator.farming.tree.TreeObservationPersistence;
import com.farmingnavigator.poh.PohProfileService;
import com.farmingnavigator.routing.capability.TravelCapabilityService;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbRunPlanner;
import com.farmingnavigator.routing.run.HerbRunCareResources;
import com.farmingnavigator.routing.run.RunTransportResourcePlanner;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.guided.GuidedRunCommand;
import com.farmingnavigator.guided.GuidedRunSession;
import com.farmingnavigator.guided.GuidedRunSnapshot;
import com.farmingnavigator.overlay.GuidedInventoryOverlay;
import com.farmingnavigator.overlay.GuidedBankOverlay;
import com.farmingnavigator.overlay.GuidedRunOverlayController;
import com.farmingnavigator.overlay.HerbSceneTracker;
import com.farmingnavigator.overlay.GuidedSceneOverlay;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.storage.StorageStateService;
import com.farmingnavigator.transport.HerbPatchTransportService;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationService;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Farming Navigator",
        description = "Account-aware farming route planner, timer, optimizer, and guided farming assistant",
        tags = {
                "farming",
                "farm",
                "herb",
                "tree",
                "route",
                "timer",
                "xp",
                "transport"
        }
)
public class FarmingNavigatorPlugin
        extends Plugin
{
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private FarmingNavigatorConfig config;

    @Inject
    private AccountStateService
            accountStateService;

    @Inject
    private HerbPatchService
            herbPatchService;

    @Inject
    private HerbPatchTransportService
            herbPatchTransportService;

    @Inject
    private StorageStateService
            storageStateService;

    @Inject
    private TravelCapabilityService
            travelCapabilityService;

    @Inject
    private TransportVerificationService
            transportVerificationService;

    @Inject
    private PohProfileService
            pohProfileService;

    @Inject
    private HerbPatchObservationService herbPatchObservationService;
    @Inject private TreePatchObservationService treePatchObservationService;
    @Inject private com.farmingnavigator.farming.garden.GardenPatchObservationService gardenPatchObservationService;
    @Inject private com.farmingnavigator.farming.field.FieldPatchObservationService fieldPatchObservationService;

    private FarmingNavigatorPanel panel;

    private NavigationButton navigationButton;
    private long lastPatchTimerRefreshMinute = -1;
    private final GuidedRunSession guidedRunSession = new GuidedRunSession();
    private HerbRunPlan latestSuggestedRun;
    private final com.farmingnavigator.guided.tree.TreeRunSession treeRunSession = new com.farmingnavigator.guided.tree.TreeRunSession();
    private com.farmingnavigator.routing.run.TreeRunPlan latestTreeRun;
    private final com.farmingnavigator.guided.garden.GardenRunSession gardenRunSession = new com.farmingnavigator.guided.garden.GardenRunSession();
    private com.farmingnavigator.routing.run.GardenRunPlan latestGardenRun;
    private final com.farmingnavigator.guided.field.FieldRunSession fieldRunSession = new com.farmingnavigator.guided.field.FieldRunSession();
    private com.farmingnavigator.routing.run.FieldRunPlan latestFieldRun;
    private final com.farmingnavigator.analytics.RunMetricsTracker runMetrics = new com.farmingnavigator.analytics.RunMetricsTracker();
    private final HerbAlertTracker herbAlertTracker = new HerbAlertTracker();
    private final com.farmingnavigator.farming.tree.TreeAlertTracker treeAlertTracker = new com.farmingnavigator.farming.tree.TreeAlertTracker();
    private final com.farmingnavigator.farming.garden.GardenAlertTracker gardenAlertTracker = new com.farmingnavigator.farming.garden.GardenAlertTracker();
    private final com.farmingnavigator.farming.field.FieldAlertTracker fieldAlertTracker = new com.farmingnavigator.farming.field.FieldAlertTracker();
    @Inject private Notifier notifier;
    @Inject private net.runelite.client.game.ItemManager itemManager;
    @Inject private com.farmingnavigator.contracts.FarmingContractService farmingContractService;
    @Inject private ConfigManager configManager;

    @Inject private OverlayManager overlayManager;
    @Inject private GuidedInventoryOverlay guidedInventoryOverlay;
    @Inject private GuidedBankOverlay guidedBankOverlay;
    @Inject private GuidedRunOverlayController guidedRunOverlayController;
    @Inject private HerbSceneTracker herbSceneTracker;
    @Inject private com.farmingnavigator.overlay.TreeSceneTracker treeSceneTracker;
    @Inject private com.farmingnavigator.overlay.GardenSceneTracker gardenSceneTracker;
    @Inject private com.farmingnavigator.overlay.FieldSceneTracker fieldSceneTracker;
    @Inject private GuidedSceneOverlay guidedSceneOverlay;

    @Override
    protected void startUp()
    {
        lastPatchTimerRefreshMinute = -1;
        latestSuggestedRun = null;
        latestTreeRun = null;
        latestGardenRun = null;
        latestFieldRun = null;
        runMetrics.clear();
        herbAlertTracker.reset();
        treeAlertTracker.reset();
        gardenAlertTracker.reset();
        fieldAlertTracker.reset();
        guidedRunSession.cancel();
        treeRunSession.cancel();
        gardenRunSession.cancel();
        fieldRunSession.cancel();
        guidedRunOverlayController.clear();
        overlayManager.add(guidedInventoryOverlay);
        overlayManager.add(guidedBankOverlay);
        overlayManager.add(guidedSceneOverlay);
        herbPatchObservationService.reset();
        treePatchObservationService.reset();
        gardenPatchObservationService.reset();
        fieldPatchObservationService.reset();
        storageStateService
                .invalidateBankSnapshot();

        panel =
                new FarmingNavigatorPanel(
                        () ->
                                clientThread.invokeLater(
                                        this::updateAccountState
                                ),
                        pohProfileService,
                        command -> clientThread.invokeLater(() -> handleGuidedRunCommand(command))
                );

        panel.setTreeCommands(command -> clientThread.invokeLater(() -> handleTreeRunCommand(command)));
        panel.setGardenCommands(command -> clientThread.invokeLater(() -> handleGardenRunCommand(command)));
        panel.setFieldCommands(command -> clientThread.invokeLater(() -> handleFieldRunCommand(command)));
        panel.setContractPreparation(crop -> clientThread.invokeLater(() -> prepareContractRun(crop)));
        navigationButton =
                NavigationButton.builder()
                        .tooltip(
                                "Farming Navigator"
                        )
                        .icon(
                                createNavigationIcon()
                        )
                        .priority(7)
                        .panel(panel)
                        .build();

        clientToolbar.addNavigation(
                navigationButton
        );

        clientThread.invokeLater(() -> {
            initializeSceneIfActive();
            updateAccountState();
        });
    }

    @Override
    protected void shutDown()
    {
        herbAlertTracker.reset();
        treeAlertTracker.reset();
        gardenAlertTracker.reset();
        fieldAlertTracker.reset();
        overlayManager.remove(guidedInventoryOverlay);
        overlayManager.remove(guidedBankOverlay);
        overlayManager.remove(guidedSceneOverlay);
        herbSceneTracker.clear();
        treeSceneTracker.clear();
        gardenSceneTracker.clear();
        fieldSceneTracker.clear();
        guidedRunOverlayController.clear();
        guidedRunSession.cancel();
        treeRunSession.cancel();
        gardenRunSession.cancel();
        fieldRunSession.cancel();
        herbPatchObservationService.reset();
        treePatchObservationService.reset();
        gardenPatchObservationService.reset();
        fieldPatchObservationService.reset();
        storageStateService
                .invalidateBankSnapshot();

        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(
                    navigationButton
            );
        }

        navigationButton = null;
        panel = null;
        runMetrics.clear();
        latestSuggestedRun = null;
        latestTreeRun = null;
        latestGardenRun = null;
        latestFieldRun = null;
    }

    @Subscribe
    public void onGameStateChanged(
            GameStateChanged event
    )
    {
        GameState state =
                event.getGameState();

        if (state == GameState.LOADING)
        {
            herbSceneTracker.clear();
            treeSceneTracker.clear();
            gardenSceneTracker.clear();
            fieldSceneTracker.clear();
            herbPatchObservationService.invalidateRegion();
            treePatchObservationService.invalidateRegion();
            gardenPatchObservationService.invalidateRegion();
            fieldPatchObservationService.invalidateRegion();
        }
        if (state == GameState.LOGGED_IN)
        {
            clientThread.invokeLater(this::initializeSceneIfActive);
        }

        if (
                state == GameState.LOGIN_SCREEN
                        || state == GameState.LOGGING_IN
                        || state == GameState.HOPPING
                        || state == GameState.CONNECTION_LOST
        )
        {
            guidedRunSession.cancel();
            treeRunSession.cancel();
            gardenRunSession.cancel();
            fieldRunSession.cancel();
            latestSuggestedRun = null;
            latestTreeRun = null;
            latestGardenRun = null;
            latestFieldRun = null;
            runMetrics.clear();
            guidedRunOverlayController.clear();
            herbSceneTracker.clear();
            treeSceneTracker.clear();
            gardenSceneTracker.clear();
            fieldSceneTracker.clear();
            herbPatchObservationService.reset();
            treePatchObservationService.reset();
            gardenPatchObservationService.reset();
            fieldPatchObservationService.reset();
            herbAlertTracker.reset();
            treeAlertTracker.reset();
            gardenAlertTracker.reset();
            fieldAlertTracker.reset();
            storageStateService
                    .invalidateBankSnapshot();
        }

        clientThread.invokeLater(
                this::updateAccountState
        );
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        herbAlertTracker.reset();
        treeAlertTracker.reset();
        gardenAlertTracker.reset();
        fieldAlertTracker.reset();
        // A mode/account switch must never retain another character's observations or active checklist.
        herbPatchObservationService.reset();
        treePatchObservationService.reset();
        gardenPatchObservationService.reset();
        fieldPatchObservationService.reset();
        guidedRunSession.cancel();
        treeSceneTracker.clear();
        gardenSceneTracker.clear();
        fieldSceneTracker.clear();
        guidedRunOverlayController.clear();
        runMetrics.clear();
        treeRunSession.cancel();
        gardenRunSession.cancel();
        fieldRunSession.cancel();
        storageStateService.invalidateBankSnapshot();
        latestSuggestedRun = null;
        latestTreeRun = null;
        latestGardenRun = null;
        latestFieldRun = null;
        clientThread.invokeLater(this::updateAccountState);
    }

    @Subscribe public void onGameObjectSpawned(GameObjectSpawned event)
    {
        herbSceneTracker.spawned(event.getGameObject()); treeSceneTracker.spawned(event.getGameObject()); gardenSceneTracker.spawned(event.getGameObject());
        fieldSceneTracker.spawned(event.getGameObject());
    }
    @Subscribe public void onGameObjectDespawned(GameObjectDespawned event)
    {
        herbSceneTracker.despawned(event.getGameObject()); treeSceneTracker.despawned(event.getGameObject()); gardenSceneTracker.despawned(event.getGameObject());
        fieldSceneTracker.despawned(event.getGameObject());
    }
    @Subscribe public void onNpcSpawned(NpcSpawned event)
    {
        herbSceneTracker.spawned(event.getNpc()); treeSceneTracker.spawned(event.getNpc()); gardenSceneTracker.spawned(event.getNpc());
        fieldSceneTracker.spawned(event.getNpc());
    }
    @Subscribe public void onNpcDespawned(NpcDespawned event)
    {
        herbSceneTracker.despawned(event.getNpc()); treeSceneTracker.despawned(event.getNpc()); gardenSceneTracker.despawned(event.getNpc());
        fieldSceneTracker.despawned(event.getNpc());
    }
    @Subscribe public void onNpcChanged(NpcChanged event)
    {
        herbSceneTracker.despawned(event.getNpc());
        herbSceneTracker.spawned(event.getNpc());
        treeSceneTracker.despawned(event.getNpc()); gardenSceneTracker.despawned(event.getNpc()); treeSceneTracker.spawned(event.getNpc()); gardenSceneTracker.spawned(event.getNpc());
        fieldSceneTracker.despawned(event.getNpc()); fieldSceneTracker.spawned(event.getNpc());
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        boolean changed = herbPatchObservationService.sample();
        changed |= treePatchObservationService.sample();
        changed |= gardenPatchObservationService.sample();
        changed |= fieldPatchObservationService.sample();
        if (fieldRunSession.snapshot(Instant.now()).isActive())
        {
            changed |= fieldRunSession.observe(herbPatchObservationService.currentLocation(), fieldPatchObservationService.snapshot(), Instant.now());
        }
        if (gardenRunSession.snapshot(Instant.now()).isActive())
        {
            changed |= gardenRunSession.observe(herbPatchObservationService.currentLocation(), gardenPatchObservationService.snapshot(), Instant.now());
        }
        if (treeRunSession.snapshot(Instant.now()).isActive())
        {
            changed |= treeRunSession.observe(herbPatchObservationService.currentLocation(),
                    treePatchObservationService.snapshot(), Instant.now());
        }
        if (guidedRunSession.snapshot().isActive())
        {
            changed |= guidedRunSession.observe(herbPatchObservationService.currentLocation(),
                    herbPatchObservationService.snapshot(), Instant.now());
            changed |= guidedRunSession.observeInventory(storageStateService.getFreeInventorySlots(),
                    herbPatchObservationService.currentLocation(), Instant.now());
        }
        long minute = Instant.now().getEpochSecond() / 60;
        if (changed || (minute != lastPatchTimerRefreshMinute
                && (herbPatchObservationService.hasObservations() || treePatchObservationService.hasObservations()
                        || gardenPatchObservationService.hasObservations() || fieldPatchObservationService.hasObservations()
                        || guidedRunSession.snapshot().isActive() || treeRunSession.snapshot(Instant.now()).isActive()
                        || gardenRunSession.snapshot(Instant.now()).isActive() || fieldRunSession.snapshot(Instant.now()).isActive())))
        {
            lastPatchTimerRefreshMinute = minute;
            herbAlertTracker.poll(herbPatchObservationService.snapshot(), config.herbRunPatches(),
                    Instant.now(), config.herbObservedAlerts(), config.herbEstimatedAlerts())
                    .forEach(notifier::notify);
            java.util.Set<TreePatchDefinition> treeAlertPatches = java.util.EnumSet.noneOf(TreePatchDefinition.class);
            for (TreePatchDefinition patch : config.treeRunPatches())
            {
                if (patch.getFamily() == config.treeRunCrop().getCrop().getFamily()) { treeAlertPatches.add(patch); }
            }
            treeAlertTracker.poll(treePatchObservationService.snapshot(), treeAlertPatches, Instant.now(),
                    config.treeObservedAlerts(), config.treeEstimatedAlerts()).forEach(notifier::notify);
            java.util.Set<com.farmingnavigator.farming.garden.GardenPatchDefinition> gardenAlertPatches =
                    java.util.EnumSet.noneOf(com.farmingnavigator.farming.garden.GardenPatchDefinition.class);
            for (com.farmingnavigator.farming.garden.GardenPatchDefinition patch : config.gardenRunPatches())
            {
                if (patch.getFamily() == config.gardenRunCrop().getCrop().getFamily()) { gardenAlertPatches.add(patch); }
            }
            gardenAlertTracker.poll(gardenPatchObservationService.snapshot(), gardenAlertPatches, Instant.now(),
                    config.gardenObservedAlerts(), config.gardenEstimatedAlerts()).forEach(notifier::notify);
            java.util.Set<com.farmingnavigator.farming.field.FieldPatchDefinition> fieldAlertPatches =
                    java.util.EnumSet.noneOf(com.farmingnavigator.farming.field.FieldPatchDefinition.class);
            for (com.farmingnavigator.farming.field.FieldPatchDefinition patch : config.fieldRunPatches())
            {
                if (patch.getFamily() == config.fieldRunCrop().getCrop().getFamily()) { fieldAlertPatches.add(patch); }
            }
            fieldAlertTracker.poll(fieldPatchObservationService.snapshot(), fieldAlertPatches, Instant.now(),
                    config.fieldObservedAlerts(), config.fieldEstimatedAlerts()).forEach(notifier::notify);
            updateAccountState();
        }
    }

    @Subscribe
    public void onStatChanged(
            StatChanged event
    )
    {
        Skill skill =
                event.getSkill();

        if (
                skill == Skill.FARMING
                        || skill == Skill.ATTACK
                        || skill == Skill.WOODCUTTING
                        || skill == Skill.MAGIC
                        || skill == Skill.AGILITY
                        || skill == Skill.HUNTER
        )
        {
            clientThread.invokeLater(
                    this::updateAccountState
            );
        }
    }

    @Subscribe
    public void onVarbitChanged(
            VarbitChanged event
    )
    {
        if (
                !isRelevantStateVarbit(
                        event.getVarbitId()
                )
        )
        {
            return;
        }

        clientThread.invokeLater(
                this::updateAccountState
        );
    }

    @Subscribe
    public void onConfigChanged(
            ConfigChanged event
    )
    {
        if (event.getKey() != null && (event.getKey().startsWith(HerbObservationPersistence.KEY_PREFIX)
                || event.getKey().startsWith(TreeObservationPersistence.KEY_PREFIX)
                || event.getKey().startsWith(com.farmingnavigator.farming.garden.GardenObservationPersistence.KEY_PREFIX)
                || event.getKey().startsWith(com.farmingnavigator.farming.field.FieldObservationPersistence.KEY_PREFIX)))
        {
            return; // Observation sampling already refreshes the panel once per changed snapshot.
        }
        if (
                !FarmingNavigatorConfig.GROUP
                        .equals(
                                event.getGroup()
                        )
        )
        {
            return;
        }

        clientThread.invokeLater(() -> {
            if (isTreeRunSetupKey(event.getKey())) { treeRunSession.cancel(); }
            if (isGardenRunSetupKey(event.getKey())) { gardenRunSession.cancel(); }
            if (isFieldRunSetupKey(event.getKey())) { fieldRunSession.cancel(); }
            if (isRunSetupKey(event.getKey()))
            {
                guidedRunSession.cancel();
                guidedRunOverlayController.clear();
            }
            updateAccountState();
        });
    }

    @Subscribe
    public void onWidgetLoaded(
            WidgetLoaded event
    )
    {
        if (
                event.getGroupId()
                        != InterfaceID.BANKMAIN
        )
        {
            return;
        }

        storageStateService
                .setBankOpen(true);

        clientThread.invokeLater(
                () ->
                {
                    storageStateService
                            .synchronizeBank();

                    updateAccountState();
                }
        );
    }

    @Subscribe
    public void onWidgetClosed(
            WidgetClosed event
    )
    {
        if (
                event.getGroupId()
                        != InterfaceID.BANKMAIN
        )
        {
            return;
        }

        storageStateService
                .setBankOpen(false);

        clientThread.invokeLater(
                this::updateAccountState
        );
    }

    @Subscribe
    public void onItemContainerChanged(
            ItemContainerChanged event
    )
    {
        int containerId =
                event.getContainerId();

        if (
                containerId == InventoryID.BANK
                        && storageStateService
                        .isBankOpen()
        )
        {
            storageStateService
                    .synchronizeBank();

            clientThread.invokeLater(
                    this::updateAccountState
            );

            return;
        }

        if (
                containerId == InventoryID.INV
                        || containerId
                        == InventoryID.WORN
        )
        {
            clientThread.invokeLater(
                    this::updateAccountState
            );
        }
    }

    static boolean isTreeRunSetupKey(String key)
    {
        return "treeRunCrop".equals(key) || "treeRunPatches".equals(key) || "treeRunProtection".equals(key)
                || "treeTransportPreference".equals(key);
    }

    static boolean isRunSetupKey(String key)
    {
        return "herbRunCrop".equals(key) || "herbRunPatches".equals(key) || "herbRunCompost".equals(key)
                || "herbTransportPreference".equals(key) || "herbHouseEntryMode".equals(key) || "herbRunCure".equals(key)
                || "herbAshTomeRead".equals(key);
    }

    static boolean isRelevantStateVarbit(
            int varbitId
    )
    {
        switch (varbitId)
        {
            case VarbitID.SPELLBOOK:
            case VarbitID.FARMGUILD_CONTRACT_TYPE:
            case VarbitID.FARMGUILD_CONTRACT_COMPLETE:

            case VarbitID.RUNE_POUCH_QUANTITY_1:
            case VarbitID.RUNE_POUCH_QUANTITY_2:
            case VarbitID.RUNE_POUCH_QUANTITY_3:
            case VarbitID.RUNE_POUCH_QUANTITY_4:
            case VarbitID.RUNE_POUCH_QUANTITY_5:
            case VarbitID.RUNE_POUCH_QUANTITY_6:

            case VarbitID.RUNE_POUCH_TYPE_1:
            case VarbitID.RUNE_POUCH_TYPE_2:
            case VarbitID.RUNE_POUCH_TYPE_3:
            case VarbitID.RUNE_POUCH_TYPE_4:
            case VarbitID.RUNE_POUCH_TYPE_5:
            case VarbitID.RUNE_POUCH_TYPE_6:

                /*
                 * Exact Fairytale II fairy-ring access state.
                 */
            case VarbitID.FAIRY2_QUEENCURE_QUEST:
            case VarbitID.LUMBRIDGE_CABBAGE_TELEPORT:
            case VarbitID.LUMBRIDGE_MEDIUM_REWARD:
            case VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE:
            case VarbitID.SEERS_SHERLOCK_TELEPORT:
            case VarbitID.VARLAMORE_VISITED:
            case VarbitID.FROG_QUEST:
            case VarbitID.FROG_QUEST_PATCH_UNLOCKED:
                return true;

            default:
                return false;
        }
    }

    private void updateAccountState()
    {
        FarmingNavigatorPanel targetPanel = panel;
        if (targetPanel == null)
        {
            return;
        }

        AccountState accountState =
                accountStateService
                        .createSnapshot();

        TravelCapabilitySnapshot
                travelCapabilitySnapshot =
                travelCapabilityService
                        .createSnapshot();

        StorageSnapshot storageSnapshot =
                storageStateService
                        .createSnapshot();

        List<HerbPatchStatus> patchStatuses =
                herbPatchService.evaluate(
                        accountState
                );

        Map<
                HerbPatchDefinition,
                List<TransportOption>
                > transportOptions =
                herbPatchTransportService
                        .getAllOptions();

        Map<
                HerbPatchDefinition,
                List<VerifiedTransportResult>
                > verifiedTransportResults =
                transportVerificationService
                        .evaluateAll(
                                accountState,
                                config,
                                travelCapabilitySnapshot,
                                storageSnapshot,
                                transportOptions
                        );

        Map<HerbPatchDefinition, HerbPatchObservation> observations =
                herbPatchObservationService.snapshot();
        Map<TreePatchDefinition, TreePatchObservation> treeObservations = treePatchObservationService.snapshot();
        Map<com.farmingnavigator.farming.garden.GardenPatchDefinition, com.farmingnavigator.farming.garden.GardenPatchObservation> gardenObservations = gardenPatchObservationService.snapshot();
        Map<com.farmingnavigator.farming.field.FieldPatchDefinition, com.farmingnavigator.farming.field.FieldPatchObservation> fieldObservations = fieldPatchObservationService.snapshot();

        HerbRunPlan suggested = new HerbRunPlanner().plan(accountState, patchStatuses,
                config.herbRunPatches(), config.herbRunCrop(), config.herbRunCompost(),
                observations, verifiedTransportResults, storageSnapshot, Instant.now(), config.herbTransportPreference());
        RequirementContext runContext = new RequirementContext(accountState, config, travelCapabilitySnapshot, storageSnapshot);
        Map<com.farmingnavigator.farming.field.FieldPatchDefinition, List<VerifiedTransportResult>> fieldTransports = transportVerificationService.evaluateAll(accountState,
                config, travelCapabilitySnapshot, storageSnapshot, new com.farmingnavigator.transport.FieldPatchTransportService().getAllOptions());
        com.farmingnavigator.routing.run.FieldRunPlan fieldRunPlan = new com.farmingnavigator.routing.run.FieldRunPlanner().plan(accountState,
                config.fieldRunPatches(), config.fieldRunCrop(), config.fieldRunMode(), config.fieldRunProtection(), config.fieldRunCompost(), fieldObservations, storageSnapshot, Instant.now())
                .withTransports(fieldTransports, config.fieldTransportPreference(), runContext);
        Map<com.farmingnavigator.farming.garden.GardenPatchDefinition, List<VerifiedTransportResult>> gardenTransports = transportVerificationService.evaluateAll(accountState,
                config, travelCapabilitySnapshot, storageSnapshot, new com.farmingnavigator.transport.GardenPatchTransportService().getAllOptions());
        com.farmingnavigator.routing.run.GardenRunPlan gardenRunPlan = new com.farmingnavigator.routing.run.GardenRunPlanner().plan(accountState,
                config.gardenRunPatches(), config.gardenRunCrop(), config.gardenRunMode(), config.gardenRunProtection(), gardenObservations, storageSnapshot, Instant.now())
                .withTransports(gardenTransports, config.gardenTransportPreference(), runContext);
        Map<TreePatchDefinition, List<VerifiedTransportResult>> treeTransports = transportVerificationService.evaluateAll(accountState,
                config, travelCapabilitySnapshot, storageSnapshot, new com.farmingnavigator.transport.TreePatchTransportService().getAllOptions());
        com.farmingnavigator.routing.run.TreeRunPlan treeRunPlan = new com.farmingnavigator.routing.run.TreeRunPlanner()
                .plan(accountState, config.treeRunPatches(), config.treeRunCrop(), config.treeRunProtection(),
                        treeObservations, storageSnapshot, Instant.now())
                .withTransports(treeTransports, config.treeTransportPreference(), runContext);
        latestFieldRun = fieldRunPlan;
        fieldRunSession.refreshTransports(fieldTransports, config.fieldTransportPreference(), runContext);
        com.farmingnavigator.guided.field.FieldRunSnapshot fieldGuidedRun = fieldRunSession.snapshot(Instant.now());
        latestGardenRun = gardenRunPlan;
        gardenRunSession.refreshTransports(gardenTransports, config.gardenTransportPreference(), runContext);
        com.farmingnavigator.guided.garden.GardenRunSnapshot gardenGuidedRun = gardenRunSession.snapshot(Instant.now());
        latestTreeRun = treeRunPlan;
        treeRunSession.refreshTransports(treeTransports, config.treeTransportPreference(), runContext);
        com.farmingnavigator.guided.tree.TreeRunSnapshot treeGuidedRun = treeRunSession.snapshot(Instant.now());
        HerbRunCareResources care = HerbRunCareResources.evaluate(suggested, config.herbRunCure(), config.herbRunCompost(), runContext);
        HerbRunPlan runPlan = suggested.withCareResources(care).withTransportResources(new RunTransportResourcePlanner()
                .evaluate(suggested.getStops(), runContext, care.getRunes()));

        latestSuggestedRun = runPlan;
        com.farmingnavigator.economics.FarmingMaterialEstimator materialEstimator = new com.farmingnavigator.economics.FarmingMaterialEstimator();
        com.farmingnavigator.economics.FarmingMaterialEstimate herbMaterials = config.showFarmingMaterialValues() && accountState.isLoggedIn()
                ? materialEstimator.herbs(runPlan, config.herbRunCompost(), itemManager::getItemPrice) : null;
        com.farmingnavigator.economics.FarmingMaterialEstimate treeMaterials = config.showFarmingMaterialValues() && accountState.isLoggedIn()
                ? materialEstimator.trees(treeRunPlan, config.treeRunProtection(), itemManager::getItemPrice) : null;
        com.farmingnavigator.economics.FarmingMaterialEstimate gardenMaterials = config.showFarmingMaterialValues() && accountState.isLoggedIn()
                ? materialEstimator.gardens(gardenRunPlan, itemManager::getItemPrice) : null;
        com.farmingnavigator.economics.FarmingMaterialEstimate fieldMaterials = config.showFarmingMaterialValues() && accountState.isLoggedIn()
                ? materialEstimator.fields(fieldRunPlan, itemManager::getItemPrice) : null;
        guidedRunSession.refreshTransports(verifiedTransportResults, config.herbTransportPreference());
        GuidedRunSnapshot guidedRun = guidedRunSession.snapshot();
        if (runMetrics.isRecording())
        {
            int xp = accountStateService.currentFarmingExperience();
            Instant metricsTime = Instant.now();
            runMetrics.sample(xp, metricsTime);
            if (!guidedRun.isActive() && !treeGuidedRun.isActive() && !gardenGuidedRun.isActive() && !fieldGuidedRun.isActive())
            {
                runMetrics.finish(xp, metricsTime, guidedRun.getStep() == com.farmingnavigator.guided.GuidedRunStep.COMPLETE
                        || treeGuidedRun.getStep() == com.farmingnavigator.guided.tree.TreeRunStep.COMPLETE
                        || gardenGuidedRun.getStep() == com.farmingnavigator.guided.garden.GardenRunStep.COMPLETE
                        || fieldGuidedRun.getStep() == com.farmingnavigator.guided.field.FieldRunStep.COMPLETE);
            }
        }
        com.farmingnavigator.analytics.RunMetricsSnapshot metricsSnapshot = runMetrics.snapshot(Instant.now());
        com.farmingnavigator.contracts.FarmingContractSnapshot farmingContract = farmingContractService.snapshot();
        if (fieldGuidedRun.isActive())
        {
            guidedRunOverlayController.updateField(fieldGuidedRun, fieldRunPlan, travelCapabilitySnapshot, config.guidedRunOverlays());
        }
        else if (gardenGuidedRun.isActive())
        {
            guidedRunOverlayController.updateGarden(gardenGuidedRun, gardenRunPlan, travelCapabilitySnapshot, config.guidedRunOverlays());
        }
        else if (treeGuidedRun.isActive())
        {
            guidedRunOverlayController.updateTree(treeGuidedRun, treeRunPlan, travelCapabilitySnapshot, config.guidedRunOverlays());
        }
        else { guidedRunOverlayController.update(guidedRun, runPlan, travelCapabilitySnapshot, config.guidedRunOverlays()); }

        SwingUtilities.invokeLater(
                () -> {
                        targetPanel.setGuidedRun(guidedRun);
                        targetPanel.setTreeObservations(treeObservations);
                        targetPanel.setGardenObservations(gardenObservations);
                        targetPanel.setFieldObservations(fieldObservations);
                        targetPanel.setFieldRunPlan(fieldRunPlan);
                        targetPanel.setFieldMaterials(fieldMaterials);
                        targetPanel.setFieldGuidedRun(fieldGuidedRun);
                        targetPanel.setGardenRunPlan(gardenRunPlan);
                        targetPanel.setGardenMaterials(gardenMaterials);
                        targetPanel.setGardenGuidedRun(gardenGuidedRun);
                        targetPanel.setTreeRunPlan(treeRunPlan);
                        targetPanel.setTreeGuidedRun(treeGuidedRun);
                        targetPanel.setRunMetrics(metricsSnapshot);
                        targetPanel.setFarmingMaterials(herbMaterials, treeMaterials);
                        targetPanel.setFarmingContract(farmingContract);
                        targetPanel.setHerbRunPlan(runPlan);
                        targetPanel.setAccountState(
                                accountState,
                                storageSnapshot,
                                patchStatuses,
                                verifiedTransportResults,
                                config,
                                observations
                        );
                }
        );
    }

    private void handleGuidedRunCommand(GuidedRunCommand command)
    {
        if (panel == null || !accountStateService.createSnapshot().isLoggedIn())
        {
            return;
        }
        Instant now = Instant.now();
        switch (command)
        {
            case START:
                if (guidedRunSession.start(latestSuggestedRun, config.herbRunCompost(), now))
                {
                    treeRunSession.cancel(); gardenRunSession.cancel(); fieldRunSession.cancel(); runMetrics.start("Herb run", accountStateService.currentFarmingExperience(), now);
                }
                break;
            case CONFIRM: guidedRunSession.confirm(now); break;
            case SKIP: guidedRunSession.skip(now); break;
            case MARK_COMPLETE: guidedRunSession.markComplete(now); break;
            case CANCEL: guidedRunSession.cancel(); break;
            default: break;
        }
        updateAccountState();
    }

    private void handleTreeRunCommand(com.farmingnavigator.guided.tree.TreeRunCommand command)
    {
        if (panel == null || !accountStateService.createSnapshot().isLoggedIn()) { return; }
        Instant now = Instant.now();
        if (command == com.farmingnavigator.guided.tree.TreeRunCommand.START)
        {
            if (treeRunSession.start(latestTreeRun, config.treeRunProtection(), now))
            {
                guidedRunSession.cancel(); gardenRunSession.cancel(); fieldRunSession.cancel(); guidedRunOverlayController.clear();
                runMetrics.start("Tree run", accountStateService.currentFarmingExperience(), now);
            }
        }
        else { treeRunSession.command(command, now); }
        updateAccountState();
    }

    private void prepareContractRun(com.farmingnavigator.contracts.ContractCrop requested)
    {
        if (panel == null) { return; }
        Map<String, Object> settings = com.farmingnavigator.contracts.ContractRunPreparation.settings(
                farmingContractService.snapshot(), requested, guidedRunSession.snapshot().isActive()
                        || treeRunSession.snapshot(Instant.now()).isActive() || gardenRunSession.snapshot(Instant.now()).isActive()
                        || fieldRunSession.snapshot(Instant.now()).isActive());
        settings.forEach((key, value) -> configManager.setConfiguration(FarmingNavigatorConfig.GROUP, key, value));
        updateAccountState();
    }

    private void handleGardenRunCommand(com.farmingnavigator.guided.garden.GardenRunCommand command)
    {
        if (panel == null || !accountStateService.createSnapshot().isLoggedIn()) { return; }
        Instant now = Instant.now();
        if (command == com.farmingnavigator.guided.garden.GardenRunCommand.START)
        {
            if (gardenRunSession.start(latestGardenRun, config.gardenRunProtection(), now))
            {
                guidedRunSession.cancel(); treeRunSession.cancel(); fieldRunSession.cancel(); guidedRunOverlayController.clear();
                runMetrics.start("Bush/cactus run", accountStateService.currentFarmingExperience(), now);
            }
        }
        else { gardenRunSession.command(command, now); }
        updateAccountState();
    }

    private void handleFieldRunCommand(com.farmingnavigator.guided.field.FieldRunCommand command)
    {
        if (panel == null || !accountStateService.createSnapshot().isLoggedIn()) { return; }
        Instant now = Instant.now();
        if (command == com.farmingnavigator.guided.field.FieldRunCommand.START)
        {
            if (fieldRunSession.start(latestFieldRun, config.fieldRunProtection(), now))
            {
                guidedRunSession.cancel(); treeRunSession.cancel(); gardenRunSession.cancel(); guidedRunOverlayController.clear();
                runMetrics.start("Allotment/flower run", accountStateService.currentFarmingExperience(), now);
            }
        }
        else { fieldRunSession.command(command, now); }
        updateAccountState();
    }

    static boolean isFieldRunSetupKey(String key)
    {
        return "fieldRunCrop".equals(key) || "fieldRunPatches".equals(key) || "fieldRunMode".equals(key)
                || "fieldRunProtection".equals(key) || "fieldTransportPreference".equals(key) || "fieldRunCompost".equals(key);
    }

    static boolean isGardenRunSetupKey(String key)
    {
        return "gardenRunCrop".equals(key) || "gardenRunPatches".equals(key) || "gardenRunMode".equals(key)
                || "gardenRunProtection".equals(key) || "gardenTransportPreference".equals(key);
    }

    private void initializeSceneIfActive()
    {
        if (panel != null)
        {
            herbSceneTracker.initializeNearby();
            treeSceneTracker.initializeNearby();
            gardenSceneTracker.initializeNearby();
            fieldSceneTracker.initializeNearby();
        }
    }

    private BufferedImage
    createNavigationIcon()
    {
        final int size = 16;

        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                image.createGraphics();

        try
        {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            graphics.setColor(
                    new Color(
                            76,
                            175,
                            80
                    )
            );

            graphics.fillOval(
                    1,
                    1,
                    size - 2,
                    size - 2
            );

            graphics.setColor(
                    Color.WHITE
            );

            graphics.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            11
                    )
            );

            String text =
                    "F";

            FontMetrics metrics =
                    graphics.getFontMetrics();

            int x =
                    (
                            size
                                    - metrics
                                    .stringWidth(
                                            text
                                    )
                    ) / 2;

            int y =
                    (
                            (
                                    size
                                            - metrics
                                            .getHeight()
                            ) / 2
                    )
                            + metrics
                            .getAscent();

            graphics.drawString(
                    text,
                    x,
                    y
            );
        }
        finally
        {
            graphics.dispose();
        }

        return image;
    }

    @Provides
    FarmingNavigatorConfig provideConfig(
            ConfigManager configManager
    )
    {
        return configManager.getConfig(
                FarmingNavigatorConfig.class
        );
    }
}
