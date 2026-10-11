package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.AccountStateService;
import com.farmingnavigator.farming.HerbPatchService;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
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
public class FarmingNavigatorPlugin extends Plugin
{
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private AccountStateService accountStateService;

    @Inject
    private HerbPatchService herbPatchService;

    private FarmingNavigatorPanel panel;
    private NavigationButton navigationButton;

    @Override
    protected void startUp()
    {
        panel = new FarmingNavigatorPanel(
                () -> clientThread.invokeLater(
                        this::updateAccountState
                )
        );

        navigationButton =
                NavigationButton.builder()
                        .tooltip("Farming Navigator")
                        .icon(createNavigationIcon())
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
        clientThread.invokeLater(
                this::updateAccountState
        );
    }

    @Subscribe
    public void onStatChanged(
            StatChanged event
    )
    {
        if (event.getSkill() == Skill.FARMING)
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
                accountStateService.createSnapshot();

        List<HerbPatchStatus> patchStatuses =
                herbPatchService.evaluate(
                        accountState
                );

        SwingUtilities.invokeLater(
                () -> panel.setAccountState(
                        accountState,
                        patchStatuses
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
                    new Color(76, 175, 80)
            );

            graphics.fillOval(
                    1,
                    1,
                    size - 2,
                    size - 2
            );

            graphics.setColor(Color.WHITE);

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
                    (size
                            - fontMetrics
                            .stringWidth(text))
                            / 2;

            int y =
                    ((size
                            - fontMetrics.getHeight())
                            / 2)
                            + fontMetrics.getAscent();

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