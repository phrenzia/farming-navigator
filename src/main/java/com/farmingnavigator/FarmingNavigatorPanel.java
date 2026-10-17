package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.PatchCondition;
import com.farmingnavigator.ui.HerbPatchObservationText;
import com.farmingnavigator.ui.HerbGrowthTimerText;
import com.farmingnavigator.ui.HerbRunPlanPanel;
import com.farmingnavigator.ui.HerbOverviewPanel;
import com.farmingnavigator.farming.state.HerbOverview;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchObservation;
import com.farmingnavigator.ui.TreeObservationsPanel;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.guided.GuidedRunCommand;
import com.farmingnavigator.guided.GuidedRunSnapshot;
import com.farmingnavigator.ui.GuidedRunPanel;
import java.util.function.Consumer;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohJewelleryBoxTier;
import com.farmingnavigator.poh.PohPortalDestination;
import com.farmingnavigator.poh.PohProfileService;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class FarmingNavigatorPanel
        extends PluginPanel
{
    private static final Color AVAILABLE_COLOR =
            new Color(120, 220, 120);

    private static final Color LOCKED_COLOR =
            new Color(235, 110, 110);

    private static final Color DISEASE_FREE_COLOR =
            new Color(120, 190, 255);

    private static final Color MUTED_COLOR =
            new Color(180, 180, 180);

    private static final Color WARNING_COLOR =
            new Color(255, 190, 80);

    private static final Color BANK_AVAILABLE_COLOR =
            new Color(110, 190, 255);

    /*
     * This width applies only to manually wrapped body text.
     *
     * The header no longer uses fixed text widths at all.
     */
    private static final int TEXT_WRAP_WIDTH =
            180;

    private static final int EDITOR_CONTROL_WIDTH =
            188;

    private static final DateTimeFormatter SYNC_TIME_FORMAT =
            DateTimeFormatter
                    .ofPattern("HH:mm:ss")
                    .withZone(
                            ZoneId.systemDefault()
                    );

    private final Runnable refreshAction;

    private final PohProfileService pohProfileService;

    private final JLabel loginStatusLabel =
            new JLabel(
                    "Status: Waiting for login",
                    SwingConstants.CENTER
            );

    private final JLabel farmingLevelLabel =
            new JLabel(
                    "Farming level: -",
                    SwingConstants.CENTER
            );

    private final JLabel patchSummaryLabel =
            new JLabel(
                    "Herb patches: -",
                    SwingConstants.CENTER
            );

    private final JPanel patchListPanel;

    private final JScrollPane patchScrollPane;

    private boolean pohEditorOpen;
    private HerbRunPlan herbRunPlan;
    private boolean herbRunDetailsOpen;
    private boolean herbOverviewOpen;
    private com.farmingnavigator.routing.spirit.SpiritTreeDeclarations.Snapshot spiritDeclarations;
    private com.farmingnavigator.ui.PlantedSpiritAccessPanel spiritAccessPanel;
    private boolean spiritAccessOpen;
    private java.util.function.Consumer<java.util.Set<com.farmingnavigator.routing.spirit.PlantedSpiritTree>> applySpiritAccess = trees -> {};
    public void setSpiritDeclarations(com.farmingnavigator.routing.spirit.SpiritTreeDeclarations.Snapshot snapshot,
            java.util.function.Consumer<java.util.Set<com.farmingnavigator.routing.spirit.PlantedSpiritTree>> apply)
    {
        if(spiritDeclarations==null || snapshot.getRevision()!=spiritDeclarations.getRevision()
                || !java.util.Objects.equals(snapshot.getProfile(),spiritDeclarations.getProfile())) { spiritAccessPanel=null; }
        spiritDeclarations=snapshot; applySpiritAccess=apply;
    }
    private boolean treeObservationsOpen;
    private com.farmingnavigator.farming.tree.TreeFamily treeObservationFamily;
    private boolean gardenObservationsOpen;
    private boolean fieldObservationsOpen;
    private com.farmingnavigator.farming.field.FieldFamily fieldObservationFamily;
    private boolean fieldRunDetailsOpen;
    private boolean fieldOverviewOpen;
    private boolean readinessDashboardOpen;
    private java.util.function.BiConsumer<com.farmingnavigator.guided.GuidedRunCommand, Long> guardedHerbCommands;
    public void setGuardedHerbCommands(java.util.function.BiConsumer<com.farmingnavigator.guided.GuidedRunCommand, Long> commands) { guardedHerbCommands = commands; }
    private java.util.function.BiConsumer<com.farmingnavigator.guided.tree.TreeRunCommand, Long> guardedTreeCommands;
    public void setGuardedTreeCommands(java.util.function.BiConsumer<com.farmingnavigator.guided.tree.TreeRunCommand, Long> commands) { guardedTreeCommands = commands; }
    private java.util.function.BiConsumer<com.farmingnavigator.guided.garden.GardenRunCommand, Long> guardedGardenCommands;
    public void setGuardedGardenCommands(java.util.function.BiConsumer<com.farmingnavigator.guided.garden.GardenRunCommand, Long> commands) { guardedGardenCommands = commands; }
    private java.util.function.BiConsumer<com.farmingnavigator.guided.field.FieldRunCommand, Long> guardedFieldCommands;
    public void setGuardedFieldCommands(java.util.function.BiConsumer<com.farmingnavigator.guided.field.FieldRunCommand, Long> commands) { guardedFieldCommands = commands; }
    private <C> Consumer<C> checklistCommands(long revision, Consumer<C> legacy, java.util.function.BiConsumer<C, Long> guarded)
    {
        return guarded == null ? legacy : com.farmingnavigator.guided.ChecklistCommandGuard.bind(revision, guarded);
    }
    private com.farmingnavigator.guided.combined.CombinedGuidanceSession.Snapshot combinedGuidance;
    private String combinedGuidanceBlocker = "";
    private Consumer<com.farmingnavigator.guided.combined.CombinedGuidanceSession.Command> combinedCommands = command -> {};
    public void setCombinedGuidance(com.farmingnavigator.guided.combined.CombinedGuidanceSession.Snapshot snapshot, String blocker,
            Consumer<com.farmingnavigator.guided.combined.CombinedGuidanceSession.Command> commands)
    { combinedGuidance = snapshot; combinedGuidanceBlocker = blocker; combinedCommands = commands; }
    private boolean combinedPreparationOpen;
    private boolean combinedArrivalsOpen;
    private com.farmingnavigator.routing.run.CombinedRunPlan combinedRunPlan;
    private com.farmingnavigator.economics.FarmingMaterialEstimate combinedMaterials;
    public void setCombinedMaterials(com.farmingnavigator.economics.FarmingMaterialEstimate materials) { combinedMaterials=materials; }
    public void setCombinedRunPlan(com.farmingnavigator.routing.run.CombinedRunPlan plan) { combinedRunPlan = plan; }
    private com.farmingnavigator.economics.FarmingMaterialEstimate fieldMaterials;
    public void setFieldMaterials(com.farmingnavigator.economics.FarmingMaterialEstimate materials) { fieldMaterials = materials; }
    private com.farmingnavigator.guided.field.FieldRunSnapshot fieldGuidedRun;
    private Consumer<com.farmingnavigator.guided.field.FieldRunCommand> fieldCommands = command -> {};
    public void setFieldGuidedRun(com.farmingnavigator.guided.field.FieldRunSnapshot snapshot) { fieldGuidedRun = snapshot; }
    public void setFieldCommands(Consumer<com.farmingnavigator.guided.field.FieldRunCommand> commands) { fieldCommands = commands; }
    private com.farmingnavigator.routing.run.FieldRunPlan fieldRunPlan;
    public void setFieldRunPlan(com.farmingnavigator.routing.run.FieldRunPlan plan) { fieldRunPlan = plan; }
    private boolean gardenRunDetailsOpen;
    private com.farmingnavigator.guided.garden.GardenRunSnapshot gardenGuidedRun;
    private Consumer<com.farmingnavigator.guided.garden.GardenRunCommand> gardenCommands = command -> {};
    public void setGardenGuidedRun(com.farmingnavigator.guided.garden.GardenRunSnapshot snapshot) { gardenGuidedRun = snapshot; }
    public void setGardenCommands(Consumer<com.farmingnavigator.guided.garden.GardenRunCommand> commands) { gardenCommands = commands; }
    private com.farmingnavigator.routing.run.GardenRunPlan gardenRunPlan;
    private com.farmingnavigator.economics.FarmingMaterialEstimate gardenMaterials;
    public void setGardenMaterials(com.farmingnavigator.economics.FarmingMaterialEstimate materials) { gardenMaterials = materials; }
    public void setGardenRunPlan(com.farmingnavigator.routing.run.GardenRunPlan plan) { gardenRunPlan = plan; }
    private Map<com.farmingnavigator.farming.garden.GardenPatchDefinition, com.farmingnavigator.farming.garden.GardenPatchObservation> gardenObservations = Collections.emptyMap();
    public void setGardenObservations(Map<com.farmingnavigator.farming.garden.GardenPatchDefinition, com.farmingnavigator.farming.garden.GardenPatchObservation> observations)
    {
        gardenObservations = Map.copyOf(observations);
    }
    private Map<com.farmingnavigator.farming.field.FieldPatchDefinition, com.farmingnavigator.farming.field.FieldPatchObservation> fieldObservations = Collections.emptyMap();
    public void setFieldObservations(Map<com.farmingnavigator.farming.field.FieldPatchDefinition, com.farmingnavigator.farming.field.FieldPatchObservation> observations)
    {
        fieldObservations = Map.copyOf(observations);
    }
    private boolean treeRunDetailsOpen;
    private com.farmingnavigator.analytics.RunMetricsSnapshot runMetrics;
    private boolean runHistoryOpen;
    private com.farmingnavigator.analytics.RunHistoryPeriod historyPeriod = com.farmingnavigator.analytics.RunHistoryPeriod.ALL;
    private com.farmingnavigator.analytics.RunHistoryStore.Kind historyKind;
    private com.farmingnavigator.analytics.RunHistoryOutcome historyOutcome = com.farmingnavigator.analytics.RunHistoryOutcome.ALL;
    private com.farmingnavigator.ui.RunHistoryPanel runHistoryPanel;
    private java.util.List<com.farmingnavigator.analytics.RunMetricsSnapshot> runHistory = java.util.List.of();
    private Runnable clearRunHistory = () -> {};
    public void setRunHistory(java.util.List<com.farmingnavigator.analytics.RunMetricsSnapshot> entries, Runnable clear)
    {
        if (!runHistory.equals(entries)) { runHistoryPanel = null; }
        runHistory = java.util.List.copyOf(entries); clearRunHistory = clear;
    }
    private com.farmingnavigator.economics.FarmingMaterialEstimate herbMaterials;
    private com.farmingnavigator.economics.FarmingMaterialEstimate treeMaterials;
    private com.farmingnavigator.contracts.FarmingContractSnapshot farmingContract;
    private Consumer<com.farmingnavigator.contracts.ContractCrop> contractPreparation;
    public void setContractPreparation(Consumer<com.farmingnavigator.contracts.ContractCrop> prepare) { contractPreparation = prepare; }
    public void setFarmingContract(com.farmingnavigator.contracts.FarmingContractSnapshot contract) { farmingContract = contract; }
    public void setFarmingMaterials(com.farmingnavigator.economics.FarmingMaterialEstimate herbs,
            com.farmingnavigator.economics.FarmingMaterialEstimate trees) { herbMaterials = herbs; treeMaterials = trees; }
    public void setRunMetrics(com.farmingnavigator.analytics.RunMetricsSnapshot metrics) { runMetrics = metrics; }
    private com.farmingnavigator.routing.run.TreeRunPlan treeRunPlan;
    private com.farmingnavigator.guided.tree.TreeRunSnapshot treeGuidedRun;
    private Consumer<com.farmingnavigator.guided.tree.TreeRunCommand> treeCommands = command -> {};
    public void setTreeGuidedRun(com.farmingnavigator.guided.tree.TreeRunSnapshot snapshot) { treeGuidedRun = snapshot; }
    public void setTreeCommands(Consumer<com.farmingnavigator.guided.tree.TreeRunCommand> commands) { treeCommands = commands; }
    public void setTreeRunPlan(com.farmingnavigator.routing.run.TreeRunPlan plan) { treeRunPlan = plan; }
    private Map<TreePatchDefinition, TreePatchObservation> treeObservations = Collections.emptyMap();

    public void setTreeObservations(Map<TreePatchDefinition, TreePatchObservation> observations)
    {
        treeObservations = Map.copyOf(observations);
    }
    private GuidedRunSnapshot guidedRun;
    private final Consumer<GuidedRunCommand> guidedRunCommands;

    public void setGuidedRun(GuidedRunSnapshot snapshot)
    {
        guidedRun = snapshot;
    }


    public void setHerbRunPlan(HerbRunPlan plan)
    {
        herbRunPlan = plan;
    }


    private boolean scrollToTopOnNextRefresh;

    public FarmingNavigatorPanel(
            Runnable refreshAction,
            PohProfileService pohProfileService
    )
    {
        this(refreshAction, pohProfileService, command -> {});
    }

    public FarmingNavigatorPanel(Runnable refreshAction, PohProfileService pohProfileService,
            Consumer<GuidedRunCommand> guidedRunCommands)
    {
        super(false);
        this.guidedRunCommands = guidedRunCommands;

        this.refreshAction =
                refreshAction;

        this.pohProfileService =
                pohProfileService;

        setLayout(
                new BorderLayout()
        );

        setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        add(
                createHeaderPanel(),
                BorderLayout.NORTH
        );

        patchListPanel =
                new WidthTrackingPanel();

        patchListPanel.setLayout(
                new BoxLayout(
                        patchListPanel,
                        BoxLayout.Y_AXIS
                )
        );

        patchListPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        patchListPanel.setBorder(
                new EmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        patchScrollPane =
                new JScrollPane(
                        patchListPanel
                );

        patchScrollPane.setBorder(
                null
        );

        patchScrollPane.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        patchScrollPane
                .getViewport()
                .setBackground(
                        ColorScheme.DARK_GRAY_COLOR
                );

        patchScrollPane
                .setHorizontalScrollBarPolicy(
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );

        patchScrollPane
                .setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                );

        patchScrollPane
                .setWheelScrollingEnabled(
                        true
                );

        patchScrollPane
                .getVerticalScrollBar()
                .setUnitIncrement(
                        20
                );

        patchScrollPane
                .getVerticalScrollBar()
                .setBlockIncrement(
                        100
                );

        add(
                patchScrollPane,
                BorderLayout.CENTER
        );
    }

    private JPanel createHeaderPanel()
    {
        JPanel headerPanel =
                new JPanel(
                        new GridBagLayout()
                );

        headerPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        headerPanel.setBorder(
                new EmptyBorder(
                        10,
                        10,
                        8,
                        10
                )
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill =
                GridBagConstraints.HORIZONTAL;

        JLabel titleLabel =
                createFullWidthCenteredLabel(
                        "Farming Navigator",
                        Color.WHITE
                );

        constraints.gridy = 0;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        5,
                        0
                );

        headerPanel.add(
                titleLabel,
                constraints
        );

        JLabel descriptionLabel =
                createFullWidthCenteredLabel(
                        "Farming route planner",
                        ColorScheme.LIGHT_GRAY_COLOR
                );

        constraints.gridy = 1;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        10,
                        0
                );

        headerPanel.add(
                descriptionLabel,
                constraints
        );

        JPanel statePanel =
                new JPanel(
                        new GridLayout(
                                0,
                                1,
                                0,
                                5
                        )
                );

        statePanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        statePanel.setBorder(
                new EmptyBorder(
                        9,
                        8,
                        9,
                        8
                )
        );

        configureStatusLabel(
                loginStatusLabel
        );

        configureStatusLabel(
                farmingLevelLabel
        );

        configureStatusLabel(
                patchSummaryLabel
        );

        statePanel.add(
                loginStatusLabel
        );

        statePanel.add(
                farmingLevelLabel
        );

        statePanel.add(
                patchSummaryLabel
        );

        constraints.gridy = 2;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        7,
                        0
                );

        headerPanel.add(
                statePanel,
                constraints
        );

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                0,
                                0
                        )
                );

        buttonPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        JButton refreshButton =
                new JButton(
                        "Refresh account state"
                );

        refreshButton.addActionListener(
                event ->
                        refreshAction.run()
        );

        buttonPanel.add(
                refreshButton
        );

        constraints.gridy = 3;

        constraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        0
                );

        headerPanel.add(
                buttonPanel,
                constraints
        );

        return headerPanel;
    }

    private JLabel createFullWidthCenteredLabel(
            String text,
            Color color
    )
    {
        JLabel label =
                new JLabel(
                        text,
                        SwingConstants.CENTER
                );

        label.setForeground(
                color
        );

        /*
         * RuneLite's font can paint extremely close to a JLabel's
         * measured text boundary.
         *
         * These side insets ensure the first and final glyph always
         * have real drawing space instead of sitting on the clip edge.
         */
        label.setBorder(
                new EmptyBorder(
                        1,
                        8,
                        1,
                        8
                )
        );

        return label;
    }

    private void configureStatusLabel(
            JLabel label
    )
    {
        label.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        label.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        label.setBorder(
                new EmptyBorder(
                        1,
                        8,
                        1,
                        8
                )
        );
    }

    public void setAccountState(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            List<HerbPatchStatus> patchStatuses,
            Map<
                    HerbPatchDefinition,
                    List<VerifiedTransportResult>
                    > transportResultsByPatch,
            FarmingNavigatorConfig config
    )
    {
        setAccountState(accountState, storageSnapshot, patchStatuses, transportResultsByPatch,
                config, Collections.emptyMap());
    }

    public void setAccountState(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            List<HerbPatchStatus> patchStatuses,
            Map<
                    HerbPatchDefinition,
                    List<VerifiedTransportResult>
                    > transportResultsByPatch,
            FarmingNavigatorConfig config,
            Map<HerbPatchDefinition, HerbPatchObservation> observations
    )
    {
        int savedScrollPosition;

        if (scrollToTopOnNextRefresh)
        {
            savedScrollPosition = 0;

            scrollToTopOnNextRefresh =
                    false;
        }
        else
        {
            savedScrollPosition =
                    getCurrentScrollPosition();
        }

        if (
                accountState == null
                        || !accountState.isLoggedIn()
        )
        {
            showLoggedOutState(
                    savedScrollPosition
            );

            return;
        }

        showLoggedInState(
                accountState,
                storageSnapshot,
                patchStatuses,
                transportResultsByPatch,
                config,
                observations,
                savedScrollPosition
        );
    }

    private void showLoggedOutState(
            int savedScrollPosition
    )
    {
        loginStatusLabel.setText(
                "Status: Not logged in"
        );

        loginStatusLabel.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        farmingLevelLabel.setText(
                "Farming level: -"
        );

        patchSummaryLabel.setText(
                "Herb patches: -"
        );

        patchListPanel.removeAll();

        patchListPanel.add(
                createWrappedText(
                        "Log in to load your account state.",
                        ColorScheme.LIGHT_GRAY_COLOR
                )
        );

        refreshPatchPanel(
                savedScrollPosition
        );
    }

    private void showLoggedInState(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            List<HerbPatchStatus> patchStatuses,
            Map<
                    HerbPatchDefinition,
                    List<VerifiedTransportResult>
                    > transportResultsByPatch,
            FarmingNavigatorConfig config,
            Map<HerbPatchDefinition, HerbPatchObservation> observations,
            int savedScrollPosition
    )
    {
        loginStatusLabel.setText(
                "Status: Logged in"
        );

        loginStatusLabel.setForeground(
                AVAILABLE_COLOR
        );

        if (
                accountState
                        .getBoostedFarmingLevel()
                        !=
                        accountState
                                .getRealFarmingLevel()
        )
        {
            farmingLevelLabel.setText(
                    "Farming level: "
                            + accountState
                            .getRealFarmingLevel()
                            + " (boosted "
                            + accountState
                            .getBoostedFarmingLevel()
                            + ")"
            );
        }
        else
        {
            farmingLevelLabel.setText(
                    "Farming level: "
                            + accountState
                            .getRealFarmingLevel()
            );
        }

        int unlockedCount = 0;

        for (
                HerbPatchStatus status
                : patchStatuses
        )
        {
            if (
                    status.isUnlocked()
            )
            {
                unlockedCount++;
            }
        }

        patchSummaryLabel.setText(
                "Herb patches: "
                        + unlockedCount
                        + " / "
                        + patchStatuses.size()
                        + " available"
        );

        patchListPanel.removeAll();

        addStorageVerificationCard(
                storageSnapshot
        );

        patchListPanel.add(
                Box.createVerticalStrut(8)
        );

        java.time.Instant overviewTime = java.time.Instant.now();
        if (accountState.isLoggedIn() && config.showReadinessDashboard())
        {
            com.farmingnavigator.farming.state.ReadinessDashboard readiness = com.farmingnavigator.farming.state.ReadinessDashboard.at(overviewTime)
                    .herbs(patchStatuses, config.herbRunPatches(), observations)
                    .trees(accountState, config.treeRunPatches(), treeObservations)
                    .gardens(accountState, config.gardenRunPatches(), gardenObservations)
                    .fields(accountState, config.fieldRunPatches(), fieldObservations).build();
            patchListPanel.add(new com.farmingnavigator.ui.ReadinessDashboardPanel(readiness, overviewTime,
                    readinessDashboardOpen, expanded -> readinessDashboardOpen = expanded));
            patchListPanel.add(Box.createVerticalStrut(8));
        }

        if (accountState.isLoggedIn() && combinedRunPlan != null)
        {
            if (combinedGuidance != null)
            {
                patchListPanel.add(new com.farmingnavigator.ui.CombinedGuidancePanel(combinedGuidance,
                        combinedGuidanceBlocker.isEmpty(), combinedGuidanceBlocker, combinedCommands));
            }
            patchListPanel.add(new com.farmingnavigator.ui.CombinedRunPlanPanel(combinedRunPlan, combinedPreparationOpen,
                    expanded -> combinedPreparationOpen = expanded,combinedMaterials,combinedArrivalsOpen,open -> combinedArrivalsOpen=open));
            patchListPanel.add(Box.createVerticalStrut(8));
        }

        if (herbRunPlan != null)
        {
            patchListPanel.add(new GuidedRunPanel(guidedRun,
                    herbRunPlan.getBlocker().isEmpty() && !herbRunPlan.getStops().isEmpty(), checklistCommands(guidedRun == null ? -1 : guidedRun.getRevision(), guidedRunCommands, guardedHerbCommands)));
            patchListPanel.add(Box.createVerticalStrut(8));
            patchListPanel.add(new HerbRunPlanPanel(herbRunPlan, herbRunDetailsOpen,
                    expanded -> herbRunDetailsOpen = expanded, herbMaterials));
            patchListPanel.add(Box.createVerticalStrut(8));
        }

        if (config.showFarmingContract() && farmingContract != null && (accountState.getBoostedFarmingLevel() >= 45
                || farmingContract.getState() != com.farmingnavigator.contracts.FarmingContractSnapshot.State.NONE))
        {
            boolean treeGuildSelected = com.farmingnavigator.contracts.ContractRunPreparation.isGuildSelected(config.treeRunCrop(), config.treeRunPatches());
            patchListPanel.add(new com.farmingnavigator.ui.FarmingContractPanel(farmingContract,
                    config.herbRunCrop(), config.treeRunCrop(), config.herbRunPatches().contains(HerbPatchDefinition.FARMING_GUILD), treeGuildSelected,
                    guidedRun != null && guidedRun.isActive() || treeGuidedRun != null && treeGuidedRun.isActive()
                            || gardenGuidedRun != null && gardenGuidedRun.isActive()
                            || fieldGuidedRun != null && fieldGuidedRun.isActive(), contractPreparation, config.gardenRunCrop(),
                    com.farmingnavigator.contracts.ContractRunPreparation.isGuildSelected(config.gardenRunCrop(), config.gardenRunPatches()), config.fieldRunCrop(),
                    com.farmingnavigator.contracts.ContractRunPreparation.isGuildSelected(config.fieldRunCrop(), config.fieldRunPatches())));
            patchListPanel.add(Box.createVerticalStrut(8));
        }
        if(accountState.isLoggedIn() && config.allowDeclaredSpiritAccess() && spiritDeclarations!=null)
        {
            if(spiritAccessPanel==null)
            {
                java.util.function.Consumer<java.util.Set<com.farmingnavigator.routing.spirit.PlantedSpiritTree>> boundApply=applySpiritAccess;
                spiritAccessPanel=new com.farmingnavigator.ui.PlantedSpiritAccessPanel(spiritDeclarations,spiritAccessOpen,
                        expanded -> spiritAccessOpen=expanded,selected -> { spiritAccessPanel=null; boundApply.accept(selected); });
            }
            patchListPanel.add(spiritAccessPanel); patchListPanel.add(Box.createVerticalStrut(8));
        }
        if (runMetrics != null)
        {
            patchListPanel.add(new com.farmingnavigator.ui.RunMetricsPanel(runMetrics));
            patchListPanel.add(Box.createVerticalStrut(8));
        }
        if (accountState.isLoggedIn() && config.saveRunHistory())
        {
            if (runHistoryPanel == null)
            {
                runHistoryPanel = new com.farmingnavigator.ui.RunHistoryPanel(runHistory,runHistoryOpen,
                        expanded -> runHistoryOpen = expanded,clearRunHistory,overviewTime,historyPeriod,historyKind,
                        (period,kind) -> { historyPeriod = period; historyKind = kind; },historyOutcome,outcome -> historyOutcome=outcome);
            }
            runHistoryPanel.refreshTime(overviewTime);
            patchListPanel.add(runHistoryPanel);
            patchListPanel.add(Box.createVerticalStrut(8));
        }
        if (treeRunPlan != null)
        {
            patchListPanel.add(new com.farmingnavigator.ui.TreeGuidedRunPanel(treeGuidedRun,
                    treeRunPlan.getBlocker().isEmpty() && !treeRunPlan.getStops().isEmpty(), checklistCommands(treeGuidedRun == null ? -1 : treeGuidedRun.getRevision(), treeCommands, guardedTreeCommands)));
            patchListPanel.add(Box.createVerticalStrut(8));
        }
        patchListPanel.add(new com.farmingnavigator.ui.TreeRunPlanPanel(
                treeRunPlan != null ? treeRunPlan : new com.farmingnavigator.routing.run.TreeRunPlanner().plan(accountState,
                        config.treeRunPatches(), config.treeRunCrop(), config.treeRunProtection(),
                        treeObservations, storageSnapshot, overviewTime), treeRunDetailsOpen,
                expanded -> treeRunDetailsOpen = expanded, treeMaterials));
        patchListPanel.add(Box.createVerticalStrut(8));
        patchListPanel.add(new HerbOverviewPanel(HerbOverview.create(patchStatuses, config.herbRunPatches(),
                observations, overviewTime), overviewTime, herbOverviewOpen, expanded -> herbOverviewOpen = expanded));
        patchListPanel.add(Box.createVerticalStrut(8));

        if (config.showTreeObservations())
        {
            patchListPanel.add(new TreeObservationsPanel(treeObservations, overviewTime,
                    treeObservationsOpen, expanded -> treeObservationsOpen = expanded,treeObservationFamily,family -> treeObservationFamily = family));
            patchListPanel.add(Box.createVerticalStrut(8));
        }

        if (accountState.isLoggedIn() && gardenRunPlan != null)
        {
            patchListPanel.add(new com.farmingnavigator.ui.GardenGuidedRunPanel(gardenGuidedRun,
                    gardenRunPlan.getBlocker().isEmpty() && !gardenRunPlan.getStops().isEmpty(), checklistCommands(gardenGuidedRun == null ? -1 : gardenGuidedRun.getRevision(), gardenCommands, guardedGardenCommands)));
            patchListPanel.add(Box.createVerticalStrut(8));
            patchListPanel.add(new com.farmingnavigator.ui.GardenRunPlanPanel(gardenRunPlan,
                    gardenRunDetailsOpen, expanded -> gardenRunDetailsOpen = expanded, gardenMaterials));
            patchListPanel.add(Box.createVerticalStrut(8));
        }
        if (config.showGardenObservations())
        {
            patchListPanel.add(new com.farmingnavigator.ui.GardenObservationsPanel(gardenObservations, overviewTime,
                    gardenObservationsOpen, expanded -> gardenObservationsOpen = expanded));
            patchListPanel.add(Box.createVerticalStrut(8));
        }

        if (accountState.isLoggedIn() && fieldRunPlan != null)
        {
            if (config.showFieldOverview())
            {
                patchListPanel.add(new com.farmingnavigator.ui.FieldOverviewPanel(com.farmingnavigator.farming.field.FieldOverview.create(
                        accountState,config.fieldRunPatches(),config.fieldRunCrop().getCrop().getFamily(),fieldObservations,overviewTime),
                        overviewTime,fieldOverviewOpen,expanded -> fieldOverviewOpen = expanded));
                patchListPanel.add(Box.createVerticalStrut(8));
            }
            patchListPanel.add(new com.farmingnavigator.ui.FieldGuidedRunPanel(fieldGuidedRun,
                    fieldRunPlan.getBlocker().isEmpty() && !fieldRunPlan.getStops().isEmpty(), checklistCommands(fieldGuidedRun == null ? -1 : fieldGuidedRun.getRevision(), fieldCommands, guardedFieldCommands)));
            patchListPanel.add(Box.createVerticalStrut(8));
            patchListPanel.add(new com.farmingnavigator.ui.FieldRunPlanPanel(fieldRunPlan,
                    fieldRunDetailsOpen, expanded -> fieldRunDetailsOpen = expanded, fieldMaterials));
            patchListPanel.add(Box.createVerticalStrut(8));
        }
        if (config.showFieldObservations())
        {
            patchListPanel.add(new com.farmingnavigator.ui.FieldObservationsPanel(fieldObservations, overviewTime,
                    fieldObservationsOpen, expanded -> fieldObservationsOpen = expanded,fieldObservationFamily,family -> fieldObservationFamily = family));
            patchListPanel.add(Box.createVerticalStrut(8));
        }

        addPohProfileCard(
                config
        );

        if (
                pohEditorOpen
        )
        {
            patchListPanel.add(
                    Box.createVerticalStrut(8)
            );

            addPohEditorCard(
                    config
            );
        }

        patchListPanel.add(
                Box.createVerticalStrut(12)
        );

        JLabel sectionHeading =
                createSafeSingleLineLabel(
                        "Herb Patch Availability",
                        Color.WHITE
                );

        patchListPanel.add(
                sectionHeading
        );

        patchListPanel.add(
                Box.createVerticalStrut(7)
        );

        if (
                storageSnapshot.isBankVerified()
        )
        {
            patchListPanel.add(
                    createWrappedText(
                            "Verified item transport checks are active. "
                                    + "POH-aware route planning is the next step.",
                            MUTED_COLOR
                    )
            );
        }
        else
        {
            patchListPanel.add(
                    createWrappedText(
                            "Open a bank once before verified route planning.",
                            WARNING_COLOR
                    )
            );
        }

        patchListPanel.add(
                Box.createVerticalStrut(10)
        );

        for (
                HerbPatchStatus status
                : patchStatuses
        )
        {
            List<VerifiedTransportResult> results =
                    transportResultsByPatch
                            .getOrDefault(
                                    status.getPatch(),
                                    Collections.emptyList()
                            );

            patchListPanel.add(
                    createPatchRow(
                            status,
                            results,
                            storageSnapshot
                                    .isBankVerified(),
                            observations.get(status.getPatch())
                    )
            );

            patchListPanel.add(
                    Box.createVerticalStrut(8)
            );
        }

        refreshPatchPanel(
                savedScrollPosition
        );
    }

    private void addStorageVerificationCard(
            StorageSnapshot storageSnapshot
    )
    {
        JPanel panel =
                createCardPanel();

        panel.add(
                createCardHeading(
                        "Verified Storage State"
                )
        );

        panel.add(
                Box.createVerticalStrut(7)
        );

        panel.add(
                createWrappedText(
                        "\u2713 Inventory: LIVE ("
                                + storageSnapshot
                                .getInventoryDistinctItemCount()
                                + " item types)",
                        AVAILABLE_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(3)
        );

        panel.add(
                createWrappedText(
                        "\u2713 Equipment: LIVE ("
                                + storageSnapshot
                                .getEquipmentDistinctItemCount()
                                + " equipped item types)",
                        AVAILABLE_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(3)
        );

        if (
                storageSnapshot
                        .isBankVerified()
        )
        {
            String syncTime =
                    formatSyncTime(
                            storageSnapshot
                                    .getBankSyncedAt()
                    );

            panel.add(
                    createWrappedText(
                            "\u2713 Main bank: VERIFIED",
                            AVAILABLE_COLOR
                    )
            );

            panel.add(
                    Box.createVerticalStrut(3)
            );

            panel.add(
                    createWrappedText(
                            storageSnapshot
                                    .getBankDistinctItemCount()
                                    + " item types - synced "
                                    + syncTime,
                            MUTED_COLOR
                    )
            );
        }
        else
        {
            panel.add(
                    createWrappedText(
                            "\u26A0 Main bank: SYNC REQUIRED",
                            WARNING_COLOR
                    )
            );

            panel.add(
                    Box.createVerticalStrut(5)
            );

            panel.add(
                    createWrappedText(
                            "Open any normal bank once before verified route planning.",
                            WARNING_COLOR
                    )
            );
        }

        patchListPanel.add(
                panel
        );
    }

    private void addPohProfileCard(
            FarmingNavigatorConfig config
    )
    {
        JPanel panel =
                createCardPanel();

        panel.add(
                createCardHeading(
                        "Player-Owned House"
                )
        );

        panel.add(
                Box.createVerticalStrut(7)
        );

        panel.add(
                createWrappedText(
                        "House location: "
                                + config
                                .pohHouseLocation()
                                .getDisplayName(),
                        config
                                .pohHouseLocation()
                                .hasHouse()
                                ? AVAILABLE_COLOR
                                : MUTED_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(3)
        );

        panel.add(
                createWrappedText(
                        "Jewellery box: "
                                + config
                                .pohJewelleryBox()
                                .getDisplayName(),
                        Color.WHITE
                )
        );

        panel.add(
                Box.createVerticalStrut(3)
        );

        panel.add(
                createWrappedText(
                        "Fairy ring: "
                                + yesNo(
                                config.pohFairyRing()
                        ),
                        config.pohFairyRing()
                                ? AVAILABLE_COLOR
                                : MUTED_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(3)
        );

        panel.add(
                createWrappedText(
                        "Spirit tree: "
                                + yesNo(
                                config.pohSpiritTree()
                        ),
                        config.pohSpiritTree()
                                ? AVAILABLE_COLOR
                                : MUTED_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(3)
        );

        panel.add(
                createWrappedText(
                        "Mounted Xeric's talisman: "
                                + yesNo(
                                config
                                        .pohMountedXericsTalisman()
                        ),
                        config
                                .pohMountedXericsTalisman()
                                ? AVAILABLE_COLOR
                                : MUTED_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(3)
        );

        Set<PohPortalDestination> destinations =
                config.pohPortalDestinations();

        if (
                destinations == null
                        || destinations.isEmpty()
        )
        {
            panel.add(
                    createWrappedText(
                            "Portal destinations: None selected",
                            MUTED_COLOR
                    )
            );
        }
        else
        {
            panel.add(
                    createWrappedText(
                            "Portal destinations: "
                                    + formatPortalDestinations(
                                    destinations
                            ),
                            Color.WHITE
                    )
            );
        }

        panel.add(
                Box.createVerticalStrut(10)
        );

        JButton editButton =
                createWideButton(
                        pohEditorOpen
                                ? "Close POH editor"
                                : "Edit POH profile"
                );

        editButton.addActionListener(
                event ->
                {
                    pohEditorOpen =
                            !pohEditorOpen;

                    refreshAction.run();
                }
        );

        panel.add(
                editButton
        );

        patchListPanel.add(
                panel
        );
    }

    private void addPohEditorCard(
            FarmingNavigatorConfig config
    )
    {
        JPanel panel =
                createCardPanel();

        panel.add(
                createCardHeading(
                        "Edit POH Profile"
                )
        );

        panel.add(
                Box.createVerticalStrut(10)
        );

        panel.add(
                createFieldLabel(
                        "House location"
                )
        );

        panel.add(
                Box.createVerticalStrut(4)
        );

        JComboBox<PohHouseLocation>
                houseLocationBox =
                createFullWidthComboBox(
                        PohHouseLocation.values(),
                        config.pohHouseLocation()
                );

        houseLocationBox.addActionListener(
                event ->
                {
                    PohHouseLocation value =
                            (PohHouseLocation)
                                    houseLocationBox
                                            .getSelectedItem();

                    pohProfileService
                            .setHouseLocation(
                                    value
                            );
                }
        );

        panel.add(
                houseLocationBox
        );

        panel.add(
                Box.createVerticalStrut(12)
        );

        panel.add(
                createFieldLabel(
                        "Jewellery box"
                )
        );

        panel.add(
                Box.createVerticalStrut(4)
        );

        JComboBox<PohJewelleryBoxTier>
                jewelleryBox =
                createFullWidthComboBox(
                        PohJewelleryBoxTier.values(),
                        config.pohJewelleryBox()
                );

        jewelleryBox.addActionListener(
                event ->
                {
                    PohJewelleryBoxTier value =
                            (PohJewelleryBoxTier)
                                    jewelleryBox
                                            .getSelectedItem();

                    pohProfileService
                            .setJewelleryBox(
                                    value
                            );
                }
        );

        panel.add(
                jewelleryBox
        );

        panel.add(
                Box.createVerticalStrut(12)
        );

        JCheckBox fairyRing =
                createOptionCheckBox(
                        "Fairy ring",
                        config.pohFairyRing()
                );

        fairyRing.addActionListener(
                event ->
                        pohProfileService
                                .setFairyRing(
                                        fairyRing.isSelected()
                                )
        );

        panel.add(
                fairyRing
        );

        JCheckBox spiritTree =
                createOptionCheckBox(
                        "Spirit tree",
                        config.pohSpiritTree()
                );

        spiritTree.addActionListener(
                event ->
                        pohProfileService
                                .setSpiritTree(
                                        spiritTree.isSelected()
                                )
        );

        panel.add(
                spiritTree
        );

        JCheckBox xericsTalisman =
                createOptionCheckBox(
                        "Mounted Xeric's talisman",
                        config
                                .pohMountedXericsTalisman()
                );

        xericsTalisman.addActionListener(
                event ->
                        pohProfileService
                                .setMountedXericsTalisman(
                                        xericsTalisman
                                                .isSelected()
                                )
        );

        panel.add(
                xericsTalisman
        );

        JCheckBox digsitePendant =
                createOptionCheckBox(
                        "Mounted digsite pendant",
                        config
                                .pohMountedDigsitePendant()
                );

        digsitePendant.addActionListener(
                event ->
                        pohProfileService
                                .setMountedDigsitePendant(
                                        digsitePendant
                                                .isSelected()
                                )
        );

        panel.add(
                digsitePendant
        );

        panel.add(
                Box.createVerticalStrut(14)
        );

        panel.add(
                createFieldLabel(
                        "Installed portal destinations"
                )
        );

        panel.add(
                Box.createVerticalStrut(5)
        );

        panel.add(
                createWrappedText(
                        "Select every relevant destination installed in a Portal Chamber or Portal Nexus.",
                        MUTED_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(8)
        );

        EnumSet<PohPortalDestination>
                selectedDestinations =
                EnumSet.noneOf(
                        PohPortalDestination.class
                );

        Set<PohPortalDestination> current =
                config.pohPortalDestinations();

        if (
                current != null
        )
        {
            selectedDestinations.addAll(
                    current
            );
        }

        for (
                PohPortalDestination destination
                : PohPortalDestination.values()
        )
        {
            JCheckBox portalCheckBox =
                    createOptionCheckBox(
                            destination
                                    .getDisplayName(),
                            selectedDestinations
                                    .contains(
                                            destination
                                    )
                    );

            portalCheckBox.addActionListener(
                    event ->
                    {
                        if (
                                portalCheckBox
                                        .isSelected()
                        )
                        {
                            selectedDestinations.add(
                                    destination
                            );
                        }
                        else
                        {
                            selectedDestinations.remove(
                                    destination
                            );
                        }

                        pohProfileService
                                .setPortalDestinations(
                                        selectedDestinations
                                );
                    }
            );

            panel.add(
                    portalCheckBox
            );
        }

        panel.add(
                Box.createVerticalStrut(12)
        );

        panel.add(
                createWrappedText(
                        "Changes save automatically.",
                        AVAILABLE_COLOR
                )
        );

        panel.add(
                Box.createVerticalStrut(8)
        );

        JButton doneButton =
                createWideButton(
                        "Done"
                );

        doneButton.addActionListener(
                event ->
                {
                    pohEditorOpen =
                            false;

                    scrollToTopOnNextRefresh =
                            true;

                    refreshAction.run();
                }
        );

        panel.add(
                doneButton
        );

        patchListPanel.add(
                panel
        );
    }

    private JPanel createPatchRow(
            HerbPatchStatus status,
            List<VerifiedTransportResult> results,
            boolean bankVerified,
            HerbPatchObservation observation
    )
    {
        JPanel row =
                createCardPanel();

        row.add(
                createWrappedText(
                        (
                                status.isUnlocked()
                                        ? "\u2713 "
                                        : "\u2717 "
                        )
                                + status
                                .getPatch()
                                .getDisplayName(),
                        status.isUnlocked()
                                ? Color.WHITE
                                : LOCKED_COLOR
                )
        );

        row.add(
                Box.createVerticalStrut(4)
        );

        if (
                status
                        .getPatch()
                        .isAlwaysDiseaseFree()
        )
        {
            row.add(
                    createWrappedText(
                            "Disease-free patch",
                            DISEASE_FREE_COLOR
                    )
            );

            row.add(
                    Box.createVerticalStrut(3)
            );
        }

        if (
                !status.isUnlocked()
        )
        {
            StringJoiner requirements =
                    new StringJoiner(
                            ", "
                    );

            for (
                    String requirement
                    : status
                    .getMissingRequirements()
            )
            {
                requirements.add(
                        requirement
                );
            }

            row.add(
                    createWrappedText(
                            "Requires: "
                                    + requirements,
                            LOCKED_COLOR
                    )
            );

            return row;
        }

        row.add(
                createWrappedText(
                        "Patch available",
                        AVAILABLE_COLOR
                )
        );

        row.add(Box.createVerticalStrut(4));
        boolean needsAttention = observation != null
                && (observation.getState().getCondition() == PatchCondition.DISEASED
                || observation.getState().getCondition() == PatchCondition.DEAD);
        row.add(createWrappedText(HerbPatchObservationText.describe(observation, ZoneId.systemDefault()),
                needsAttention ? WARNING_COLOR : MUTED_COLOR));
        String timerText = HerbGrowthTimerText.describe(observation, Instant.now());
        if (!timerText.isEmpty())
        {
            row.add(Box.createVerticalStrut(3));
            row.add(createWrappedText(timerText, MUTED_COLOR));
        }

        if (
                !bankVerified
        )
        {
            row.add(
                    Box.createVerticalStrut(5)
            );

            row.add(
                    createWrappedText(
                            "Transport verification: bank sync required",
                            WARNING_COLOR
                    )
            );

            return row;
        }

        appendVerifiedTransports(
                row,
                results
        );

        return row;
    }

    private void appendVerifiedTransports(
            JPanel row,
            List<VerifiedTransportResult> results
    )
    {
        row.add(
                Box.createVerticalStrut(7)
        );

        if (
                results.isEmpty()
        )
        {
            row.add(
                    createWrappedText(
                            "Full route catalogue not connected yet. "
                                    + "This patch will receive a best route "
                                    + "and backup route in the next step.",
                            MUTED_COLOR
                    )
            );

            return;
        }

        row.add(
                createWrappedText(
                        "Verified item transport checks:",
                        MUTED_COLOR
                )
        );

        int numberToShow =
                Math.min(
                        2,
                        results.size()
                );

        for (
                int index = 0;
                index < numberToShow;
                index++
        )
        {
            VerifiedTransportResult result =
                    results.get(index);

            row.add(
                    Box.createVerticalStrut(7)
            );

            row.add(
                    createWrappedText(
                            (index + 1)
                                    + ". "
                                    + result
                                    .getOption()
                                    .getDisplayName(),
                            Color.WHITE
                    )
            );

            row.add(
                    Box.createVerticalStrut(3)
            );

            row.add(
                    createWrappedText(
                            result
                                    .getState()
                                    .getDisplayName(),
                            getTransportStateColor(
                                    result.getState()
                            )
                    )
            );

            row.add(
                    Box.createVerticalStrut(3)
            );

            row.add(
                    createWrappedText(
                            result.getDetail(),
                            getTransportStateColor(
                                    result.getState()
                            )
                    )
            );
        }
    }

    private JPanel createCardPanel()
    {
        JPanel panel =
                new JPanel();

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        panel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        9,
                        8,
                        12,
                        8
                )
        );

        panel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        panel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );

        return panel;
    }

    private JLabel createSafeSingleLineLabel(
            String text,
            Color color
    )
    {
        JLabel label =
                new JLabel(
                        text
                );

        label.setForeground(
                color
        );

        label.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        label.setBorder(
                new EmptyBorder(
                        0,
                        2,
                        0,
                        2
                )
        );

        return label;
    }

    private JLabel createCardHeading(
            String text
    )
    {
        return createSafeSingleLineLabel(
                text,
                Color.WHITE
        );
    }

    private JLabel createFieldLabel(
            String text
    )
    {
        return createSafeSingleLineLabel(
                text,
                ColorScheme.LIGHT_GRAY_COLOR
        );
    }

    private JPanel createWrappedText(
            String text,
            Color color
    )
    {
        JPanel textPanel =
                new JPanel();

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        textPanel.setOpaque(
                false
        );

        textPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        Font font =
                UIManager.getFont(
                        "Label.font"
                );

        if (
                font == null
        )
        {
            font =
                    new JLabel()
                            .getFont();
        }

        FontMetrics metrics =
                textPanel.getFontMetrics(
                        font
                );

        List<String> wrappedLines =
                wrapText(
                        text,
                        metrics,
                        TEXT_WRAP_WIDTH
                );

        for (
                int index = 0;
                index < wrappedLines.size();
                index++
        )
        {
            JLabel lineLabel =
                    new JLabel(
                            wrappedLines.get(index)
                    );

            lineLabel.setFont(
                    font
            );

            lineLabel.setForeground(
                    color
            );

            lineLabel.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            /*
             * Tiny horizontal side padding protects the RuneLite font's
             * glyph overhang from the JLabel clip boundary.
             */
            lineLabel.setBorder(
                    new EmptyBorder(
                            0,
                            2,
                            0,
                            2
                    )
            );

            textPanel.add(
                    lineLabel
            );

            if (
                    index
                            < wrappedLines.size() - 1
            )
            {
                textPanel.add(
                        Box.createVerticalStrut(1)
                );
            }
        }

        Dimension preferred =
                textPanel.getPreferredSize();

        textPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        preferred.height
                )
        );

        return textPanel;
    }

    private List<String> wrapText(
            String text,
            FontMetrics metrics,
            int maximumWidth
    )
    {
        List<String> lines =
                new ArrayList<>();

        if (
                text == null
                        || text.isEmpty()
        )
        {
            lines.add("");
            return lines;
        }

        String[] paragraphs =
                text.split(
                        "\\n",
                        -1
                );

        for (
                String paragraph
                : paragraphs
        )
        {
            if (
                    paragraph.isEmpty()
            )
            {
                lines.add("");
                continue;
            }

            String[] words =
                    paragraph
                            .trim()
                            .split(
                                    "\\s+"
                            );

            StringBuilder currentLine =
                    new StringBuilder();

            for (
                    String word
                    : words
            )
            {
                if (
                        currentLine.length() == 0
                )
                {
                    currentLine.append(
                            word
                    );

                    continue;
                }

                String candidate =
                        currentLine
                                + " "
                                + word;

                if (
                        metrics.stringWidth(
                                candidate
                        )
                                <= maximumWidth
                )
                {
                    currentLine
                            .append(" ")
                            .append(word);
                }
                else
                {
                    lines.add(
                            currentLine.toString()
                    );

                    currentLine =
                            new StringBuilder(
                                    word
                            );
                }
            }

            if (
                    currentLine.length() > 0
            )
            {
                lines.add(
                        currentLine.toString()
                );
            }
        }

        return lines;
    }

    private <T> JComboBox<T>
    createFullWidthComboBox(
            T[] options,
            T selected
    )
    {
        JComboBox<T> comboBox =
                new JComboBox<>(
                        options
                );

        comboBox.setSelectedItem(
                selected
        );

        comboBox.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        Dimension size =
                new Dimension(
                        EDITOR_CONTROL_WIDTH,
                        27
                );

        comboBox.setMinimumSize(
                size
        );

        comboBox.setPreferredSize(
                size
        );

        comboBox.setMaximumSize(
                size
        );

        return comboBox;
    }

    private JCheckBox createOptionCheckBox(
            String text,
            boolean selected
    )
    {
        JCheckBox checkBox =
                new JCheckBox(
                        text,
                        selected
                );

        checkBox.setOpaque(
                false
        );

        checkBox.setForeground(
                Color.WHITE
        );

        checkBox.setFocusable(
                false
        );

        checkBox.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        checkBox.setMaximumSize(
                new Dimension(
                        EDITOR_CONTROL_WIDTH,
                        27
                )
        );

        return checkBox;
    }

    private JButton createWideButton(
            String text
    )
    {
        JButton button =
                new JButton(
                        text
                );

        button.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        Dimension size =
                new Dimension(
                        EDITOR_CONTROL_WIDTH,
                        30
                );

        button.setMinimumSize(
                size
        );

        button.setPreferredSize(
                size
        );

        button.setMaximumSize(
                size
        );

        return button;
    }

    private int getCurrentScrollPosition()
    {
        return patchScrollPane
                .getVerticalScrollBar()
                .getValue();
    }

    private void refreshPatchPanel(
            int requestedScrollPosition
    )
    {
        patchListPanel.revalidate();
        patchListPanel.repaint();

        patchScrollPane.revalidate();
        patchScrollPane.repaint();

        SwingUtilities.invokeLater(
                () ->
                {
                    patchListPanel.revalidate();
                    patchListPanel.repaint();

                    restoreScrollPosition(
                            requestedScrollPosition
                    );
                }
        );
    }

    private void restoreScrollPosition(
            int requestedPosition
    )
    {
        JScrollBar scrollBar =
                patchScrollPane
                        .getVerticalScrollBar();

        int minimum =
                scrollBar.getMinimum();

        int maximum =
                scrollBar.getMaximum()
                        - scrollBar.getVisibleAmount();

        if (
                maximum < minimum
        )
        {
            maximum =
                    minimum;
        }

        int restoredPosition =
                Math.max(
                        minimum,
                        Math.min(
                                requestedPosition,
                                maximum
                        )
                );

        scrollBar.setValue(
                restoredPosition
        );
    }

    private Color getTransportStateColor(
            VerifiedTransportState state
    )
    {
        switch (
                state
        )
        {
            case READY_NOW:
                return AVAILABLE_COLOR;

            case AVAILABLE_FROM_BANK:
                return BANK_AVAILABLE_COLOR;

            case MISSING:
            case LOCKED:
            default:
                return LOCKED_COLOR;
        }
    }

    private String formatPortalDestinations(
            Set<PohPortalDestination> destinations
    )
    {
        StringJoiner joiner =
                new StringJoiner(
                        ", "
                );

        for (
                PohPortalDestination destination
                : PohPortalDestination.values()
        )
        {
            if (
                    destinations.contains(
                            destination
                    )
            )
            {
                joiner.add(
                        destination
                                .getDisplayName()
                );
            }
        }

        return joiner.toString();
    }

    private String yesNo(
            boolean value
    )
    {
        return value
                ? "Yes"
                : "No";
    }

    private String formatSyncTime(
            Instant instant
    )
    {
        if (
                instant == null
        )
        {
            return "this session";
        }

        return SYNC_TIME_FORMAT.format(
                instant
        );
    }

    private static class WidthTrackingPanel
            extends JPanel
            implements Scrollable
    {
        @Override
        public Dimension
        getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction
        )
        {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction
        )
        {
            return 100;
        }

        @Override
        public boolean
        getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean
        getScrollableTracksViewportHeight()
        {
            return false;
        }
    }
}
