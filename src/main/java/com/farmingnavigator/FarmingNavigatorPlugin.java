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

    private FarmingNavigatorPanel panel;

    private NavigationButton navigationButton;
    private long lastPatchTimerRefreshMinute = -1;
    private final GuidedRunSession guidedRunSession = new GuidedRunSession();
    private HerbRunPlan latestSuggestedRun;
    private final HerbAlertTracker herbAlertTracker = new HerbAlertTracker();
    @Inject private Notifier notifier;

    @Inject private OverlayManager overlayManager;
    @Inject private GuidedInventoryOverlay guidedInventoryOverlay;
    @Inject private GuidedBankOverlay guidedBankOverlay;
    @Inject private GuidedRunOverlayController guidedRunOverlayController;
    @Inject private HerbSceneTracker herbSceneTracker;
    @Inject private GuidedSceneOverlay guidedSceneOverlay;

    @Override
    protected void startUp()
    {
        lastPatchTimerRefreshMinute = -1;
        herbAlertTracker.reset();
        guidedRunSession.cancel();
        guidedRunOverlayController.clear();
        overlayManager.add(guidedInventoryOverlay);
        overlayManager.add(guidedBankOverlay);
        overlayManager.add(guidedSceneOverlay);
        herbPatchObservationService.reset();
        treePatchObservationService.reset();
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
        overlayManager.remove(guidedInventoryOverlay);
        overlayManager.remove(guidedBankOverlay);
        overlayManager.remove(guidedSceneOverlay);
        herbSceneTracker.clear();
        guidedRunOverlayController.clear();
        guidedRunSession.cancel();
        herbPatchObservationService.reset();
        treePatchObservationService.reset();
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
            herbPatchObservationService.invalidateRegion();
            treePatchObservationService.invalidateRegion();
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
            guidedRunOverlayController.clear();
            herbSceneTracker.clear();
            herbPatchObservationService.reset();
            treePatchObservationService.reset();
            herbAlertTracker.reset();
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
        // A mode/account switch must never retain another character's observations or active checklist.
        herbPatchObservationService.reset();
        treePatchObservationService.reset();
        guidedRunSession.cancel();
        guidedRunOverlayController.clear();
        storageStateService.invalidateBankSnapshot();
        clientThread.invokeLater(this::updateAccountState);
    }

    @Subscribe public void onGameObjectSpawned(GameObjectSpawned event) { herbSceneTracker.spawned(event.getGameObject()); }
    @Subscribe public void onGameObjectDespawned(GameObjectDespawned event) { herbSceneTracker.despawned(event.getGameObject()); }
    @Subscribe public void onNpcSpawned(NpcSpawned event) { herbSceneTracker.spawned(event.getNpc()); }
    @Subscribe public void onNpcDespawned(NpcDespawned event) { herbSceneTracker.despawned(event.getNpc()); }
    @Subscribe public void onNpcChanged(NpcChanged event)
    {
        herbSceneTracker.despawned(event.getNpc());
        herbSceneTracker.spawned(event.getNpc());
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        boolean changed = herbPatchObservationService.sample();
        changed |= treePatchObservationService.sample();
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
                        || guidedRunSession.snapshot().isActive())))
        {
            lastPatchTimerRefreshMinute = minute;
            herbAlertTracker.poll(herbPatchObservationService.snapshot(), config.herbRunPatches(),
                    Instant.now(), config.herbObservedAlerts(), config.herbEstimatedAlerts())
                    .forEach(notifier::notify);
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
                || event.getKey().startsWith(TreeObservationPersistence.KEY_PREFIX)))
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

        HerbRunPlan suggested = new HerbRunPlanner().plan(accountState, patchStatuses,
                config.herbRunPatches(), config.herbRunCrop(), config.herbRunCompost(),
                observations, verifiedTransportResults, storageSnapshot, Instant.now(), config.herbTransportPreference());
        RequirementContext runContext = new RequirementContext(accountState, config, travelCapabilitySnapshot, storageSnapshot);
        HerbRunCareResources care = HerbRunCareResources.evaluate(suggested, config.herbRunCure(), config.herbRunCompost(), runContext);
        HerbRunPlan runPlan = suggested.withCareResources(care).withTransportResources(new RunTransportResourcePlanner()
                .evaluate(suggested.getStops(), runContext, care.getRunes()));

        latestSuggestedRun = runPlan;
        guidedRunSession.refreshTransports(verifiedTransportResults, config.herbTransportPreference());
        GuidedRunSnapshot guidedRun = guidedRunSession.snapshot();
        guidedRunOverlayController.update(guidedRun, runPlan, travelCapabilitySnapshot, config.guidedRunOverlays());

        SwingUtilities.invokeLater(
                () -> {
                        targetPanel.setGuidedRun(guidedRun);
                        targetPanel.setTreeObservations(treeObservations);
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
        if (panel == null)
        {
            return;
        }
        Instant now = Instant.now();
        switch (command)
        {
            case START: guidedRunSession.start(latestSuggestedRun, config.herbRunCompost(), now); break;
            case CONFIRM: guidedRunSession.confirm(now); break;
            case SKIP: guidedRunSession.skip(now); break;
            case MARK_COMPLETE: guidedRunSession.markComplete(now); break;
            case CANCEL: guidedRunSession.cancel(); break;
            default: break;
        }
        updateAccountState();
    }

    private void initializeSceneIfActive()
    {
        if (panel != null)
        {
            herbSceneTracker.initializeNearby();
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
