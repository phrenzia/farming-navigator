package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class FarmingNavigatorPanel extends PluginPanel
{
    private static final Color AVAILABLE_COLOR =
            new Color(120, 220, 120);

    private static final Color LOCKED_COLOR =
            new Color(235, 110, 110);

    private static final Color DISEASE_FREE_COLOR =
            new Color(120, 190, 255);

    private final JLabel loginStatusLabel =
            new JLabel("Status: Waiting for login");

    private final JLabel farmingLevelLabel =
            new JLabel("Farming level: -");

    private final JLabel patchSummaryLabel =
            new JLabel("Herb patches: -");

    private final JPanel patchListPanel;

    public FarmingNavigatorPanel(Runnable refreshAction)
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel headerPanel =
                createHeaderPanel(refreshAction);

        add(headerPanel, BorderLayout.NORTH);

        patchListPanel = new JPanel();

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
                new EmptyBorder(8, 8, 8, 8)
        );

        JScrollPane scrollPane =
                new JScrollPane(patchListPanel);

        scrollPane.setBorder(null);

        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane
                .getVerticalScrollBar()
                .setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel(
            Runnable refreshAction
    )
    {
        JPanel headerPanel = new JPanel();

        headerPanel.setLayout(
                new BoxLayout(
                        headerPanel,
                        BoxLayout.Y_AXIS
                )
        );

        headerPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        headerPanel.setBorder(
                new EmptyBorder(10, 10, 8, 10)
        );

        JLabel titleLabel = new JLabel(
                "<html><div style='text-align:center'>"
                        + "<b>Farming Navigator</b>"
                        + "</div></html>",
                SwingConstants.CENTER
        );

        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel descriptionLabel = new JLabel(
                "<html><div style='text-align:center'>"
                        + "Account-aware farming planning"
                        + "</div></html>",
                SwingConstants.CENTER
        );

        descriptionLabel.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        descriptionLabel.setAlignmentX(
                CENTER_ALIGNMENT
        );

        JPanel statePanel = new JPanel(
                new GridLayout(0, 1, 0, 4)
        );

        statePanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        statePanel.setBorder(
                new EmptyBorder(8, 8, 8, 8)
        );

        loginStatusLabel.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        farmingLevelLabel.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        patchSummaryLabel.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        statePanel.add(loginStatusLabel);
        statePanel.add(farmingLevelLabel);
        statePanel.add(patchSummaryLabel);

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER
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
                event -> refreshAction.run()
        );

        buttonPanel.add(refreshButton);

        headerPanel.add(titleLabel);

        headerPanel.add(
                Box.createVerticalStrut(4)
        );

        headerPanel.add(descriptionLabel);

        headerPanel.add(
                Box.createVerticalStrut(8)
        );

        headerPanel.add(statePanel);

        headerPanel.add(
                Box.createVerticalStrut(4)
        );

        headerPanel.add(buttonPanel);

        return headerPanel;
    }

    public void setAccountState(
            AccountState accountState,
            List<HerbPatchStatus> patchStatuses
    )
    {
        if (
                accountState == null
                        || !accountState.isLoggedIn()
        )
        {
            showLoggedOutState();
            return;
        }

        showLoggedInState(
                accountState,
                patchStatuses
        );
    }

    private void showLoggedOutState()
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

        JLabel messageLabel = new JLabel(
                "<html>"
                        + "Log in to load your "
                        + "available farming patches."
                        + "</html>"
        );

        messageLabel.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        patchListPanel.add(messageLabel);

        refreshPatchPanel();
    }

    private void showLoggedInState(
            AccountState accountState,
            List<HerbPatchStatus> patchStatuses
    )
    {
        loginStatusLabel.setText(
                "Status: Logged in"
        );

        loginStatusLabel.setForeground(
                AVAILABLE_COLOR
        );

        if (
                accountState.getBoostedFarmingLevel()
                        != accountState.getRealFarmingLevel()
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
            if (status.isUnlocked())
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

        JLabel sectionLabel =
                new JLabel(
                        "Herb Patch Availability"
                );

        sectionLabel.setForeground(
                Color.WHITE
        );

        patchListPanel.add(sectionLabel);

        patchListPanel.add(
                Box.createVerticalStrut(6)
        );

        for (
                HerbPatchStatus status
                : patchStatuses
        )
        {
            patchListPanel.add(
                    createPatchRow(status)
            );

            patchListPanel.add(
                    Box.createVerticalStrut(6)
            );
        }

        refreshPatchPanel();
    }

    private JPanel createPatchRow(
            HerbPatchStatus status
    )
    {
        HerbPatchDefinition patch =
                status.getPatch();

        JPanel row = new JPanel(
                new BorderLayout(8, 0)
        );

        row.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        row.setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        row.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        90
                )
        );

        JLabel stateLabel =
                new JLabel(
                        status.isUnlocked()
                                ? "\u2713"
                                : "\u2717"
                );

        stateLabel.setForeground(
                status.isUnlocked()
                        ? AVAILABLE_COLOR
                        : LOCKED_COLOR
        );

        StringBuilder text =
                new StringBuilder();

        text.append("<html><b>")
                .append(
                        patch.getDisplayName()
                )
                .append("</b>");

        if (patch.isAlwaysDiseaseFree())
        {
            text.append(
                    "<br>"
                            + "<font color='#78BEFF'>"
                            + "Disease-free patch"
                            + "</font>"
            );
        }

        if (!status.isUnlocked())
        {
            text.append(
                    "<br>"
                            + "<font color='#EB6E6E'>"
                            + "Requires: "
            );

            for (
                    int index = 0;
                    index
                            < status
                            .getMissingRequirements()
                            .size();
                    index++
            )
            {
                if (index > 0)
                {
                    text.append(", ");
                }

                text.append(
                        status
                                .getMissingRequirements()
                                .get(index)
                );
            }

            text.append("</font>");
        }
        else
        {
            text.append(
                    "<br>"
                            + "<font color='#78DC78'>"
                            + "Available"
                            + "</font>"
            );
        }

        text.append("</html>");

        JLabel informationLabel =
                new JLabel(
                        text.toString()
                );

        informationLabel.setForeground(
                Color.WHITE
        );

        row.add(
                stateLabel,
                BorderLayout.WEST
        );

        row.add(
                informationLabel,
                BorderLayout.CENTER
        );

        return row;
    }

    private void refreshPatchPanel()
    {
        patchListPanel.revalidate();
        patchListPanel.repaint();
    }
}