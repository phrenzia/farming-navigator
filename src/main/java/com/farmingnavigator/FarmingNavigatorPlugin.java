package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.AccountStateService;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchService;
import com.farmingnavigator.farming.HerbPatchStatus;
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
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

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
    private AccountStateService accountStateService;

    @Inject
    private HerbPatchService herbPatchService;

    @Inject
    private HerbPatchTransportService herbPatchTransportService;

    @Inject
    private StorageStateService storageStateService;

    @Inject
    private TransportVerificationService transportVerificationService;

    private FarmingNavigatorPanel panel;

    private NavigationButton navigationButton;

    @Override
    protected void startUp()
    {
        storageStateService
                .invalidateBankSnapshot();

        panel =
                new FarmingNavigatorPanel(
                        () ->
                                clientThread.invokeLater(
                                        this::updateAccountState
                                )
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

        clientThread.invokeLater(
                this::updateAccountState
        );
    }

    @Override
    protected void shutDown()
    {
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

        if (
                state == GameState.LOGIN_SCREEN
                        || state == GameState.LOGGING_IN
                        || state == GameState.HOPPING
                        || state == GameState.CONNECTION_LOST
        )
        {
            storageStateService
                    .invalidateBankSnapshot();
        }

        clientThread.invokeLater(
                this::updateAccountState
        );
    }

    @Subscribe
    public void onStatChanged(
            StatChanged event
    )
    {
        if (
                event.getSkill()
                        == Skill.FARMING
        )
        {
            clientThread.invokeLater(
                    this::updateAccountState
            );
        }
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

    private void updateAccountState()
    {
        if (panel == null)
        {
            return;
        }

        AccountState accountState =
                accountStateService
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
                                storageSnapshot,
                                transportOptions
                        );

        SwingUtilities.invokeLater(
                () ->
                        panel.setAccountState(
                                accountState,
                                storageSnapshot,
                                patchStatuses,
                                verifiedTransportResults
                        )
        );
    }

    private BufferedImage createNavigationIcon()
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

            String text = "F";

            FontMetrics fontMetrics =
                    graphics.getFontMetrics();

            int x =
                    (
                            size
                                    - fontMetrics
                                    .stringWidth(
                                            text
                                    )
                    ) / 2;

            int y =
                    (
                            (
                                    size
                                            - fontMetrics
                                            .getHeight()
                            ) / 2
                    )
                            + fontMetrics
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