package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.TransportOption;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
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

    private static final Color MUTED_COLOR =
            new Color(180, 180, 180);

    private static final Color WARNING_COLOR =
            new Color(255, 190, 80);

    private static final DateTimeFormatter
            SYNC_TIME_FORMAT =
            DateTimeFormatter
                    .ofPattern("HH:mm:ss")
                    .withZone(
                            ZoneId.systemDefault()
                    );

    private final JLabel loginStatusLabel =
            new JLabel(
                    "Status: Waiting for login"
            );

    private final JLabel farmingLevelLabel =
            new JLabel(
                    "Farming level: -"
            );

    private final JLabel patchSummaryLabel =
            new JLabel(
                    "Herb patches: -"
            );

    private final JPanel patchListPanel;

    private final JScrollPane patchScrollPane;

    public FarmingNavigatorPanel(
            Runnable refreshAction
    )
    {
        super(false);

        setLayout(
                new BorderLayout()
        );

        setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        JPanel headerPanel =
                createHeaderPanel(
                        refreshAction
                );

        add(
                headerPanel,
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
                        JScrollPane
                                .HORIZONTAL_SCROLLBAR_NEVER
                );

        patchScrollPane
                .setVerticalScrollBarPolicy(
                        JScrollPane
                                .VERTICAL_SCROLLBAR_AS_NEEDED
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

    private JPanel createHeaderPanel(
            Runnable refreshAction
    )
    {
        JPanel headerPanel =
                new JPanel();

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
                new EmptyBorder(
                        10,
                        10,
                        8,
                        10
                )
        );

        JLabel titleLabel =
                new JLabel(
                        "Farming Navigator",
                        SwingConstants.CENTER
                );

        titleLabel.setForeground(
                Color.WHITE
        );

        titleLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel descriptionLabel =
                new JLabel(
                        "Account-aware farming planning",
                        SwingConstants.CENTER
                );

        descriptionLabel.setForeground(
                ColorScheme.LIGHT_GRAY_COLOR
        );

        descriptionLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JPanel statePanel =
                new JPanel(
                        new GridLayout(
                                0,
                                1,
                                0,
                                4
                        )
                );

        statePanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        statePanel.setBorder(
                new EmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        statePanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        statePanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        90
                )
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

        statePanel.add(
                loginStatusLabel
        );

        statePanel.add(
                farmingLevelLabel
        );

        statePanel.add(
                patchSummaryLabel
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

        buttonPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        buttonPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        40
                )
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

        headerPanel.add(
                titleLabel
        );

        headerPanel.add(
                Box.createVerticalStrut(4)
        );

        headerPanel.add(
                descriptionLabel
        );

        headerPanel.add(
                Box.createVerticalStrut(8)
        );

        headerPanel.add(
                statePanel
        );

        headerPanel.add(
                Box.createVerticalStrut(6)
        );

        headerPanel.add(
                buttonPanel
        );

        return headerPanel;
    }

    public void setAccountState(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            List<HerbPatchStatus> patchStatuses,
            Map<
                    HerbPatchDefinition,
                    List<TransportOption>
                    > transportOptionsByPatch
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
                storageSnapshot,
                patchStatuses,
                transportOptionsByPatch
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

        JTextArea message =
                createWrappedText(
                        "Log in to load your account state.",
                        ColorScheme.LIGHT_GRAY_COLOR
                );

        patchListPanel.add(
                message
        );

        resetScrollPosition();
        refreshPatchPanel();
    }

    private void showLoggedInState(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            List<HerbPatchStatus> patchStatuses,
            Map<
                    HerbPatchDefinition,
                    List<TransportOption>
                    > transportOptionsByPatch
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

        addStorageVerificationCard(
                storageSnapshot
        );

        patchListPanel.add(
                Box.createVerticalStrut(10)
        );

        JLabel sectionLabel =
                new JLabel(
                        "Herb Patch Availability"
                );

        sectionLabel.setForeground(
                Color.WHITE
        );

        sectionLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        sectionLabel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        25
                )
        );

        patchListPanel.add(
                sectionLabel
        );

        patchListPanel.add(
                Box.createVerticalStrut(4)
        );

        JTextArea transportNotice =
                createWrappedText(
                        "Transport methods are still baseline rankings. "
                                + "Exact item, charge, spell, and unlock verification "
                                + "will be connected after the storage engine.",
                        MUTED_COLOR
                );

        patchListPanel.add(
                transportNotice
        );

        patchListPanel.add(
                Box.createVerticalStrut(10)
        );

        for (
                HerbPatchStatus status
                : patchStatuses
        )
        {
            List<TransportOption>
                    transportOptions =
                    transportOptionsByPatch
                            .getOrDefault(
                                    status.getPatch(),
                                    Collections.emptyList()
                            );

            JPanel row =
                    createPatchRow(
                            status,
                            transportOptions
                    );

            patchListPanel.add(
                    row
            );

            patchListPanel.add(
                    Box.createVerticalStrut(8)
            );
        }

        refreshPatchPanel();
    }

    private void addStorageVerificationCard(
            StorageSnapshot storageSnapshot
    )
    {
        JPanel storagePanel =
                new JPanel();

        storagePanel.setLayout(
                new BoxLayout(
                        storagePanel,
                        BoxLayout.Y_AXIS
                )
        );

        storagePanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        storagePanel.setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        storagePanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        storagePanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );

        JLabel heading =
                new JLabel(
                        "Verified Storage State"
                );

        heading.setForeground(
                Color.WHITE
        );

        heading.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        storagePanel.add(
                heading
        );

        storagePanel.add(
                Box.createVerticalStrut(5)
        );

        JTextArea inventoryStatus =
                createWrappedText(
                        "\u2713 Inventory: LIVE"
                                + " ("
                                + storageSnapshot
                                .getInventoryDistinctItemCount()
                                + " item types)",
                        AVAILABLE_COLOR
                );

        storagePanel.add(
                inventoryStatus
        );

        JTextArea equipmentStatus =
                createWrappedText(
                        "\u2713 Equipment: LIVE"
                                + " ("
                                + storageSnapshot
                                .getEquipmentDistinctItemCount()
                                + " equipped item types)",
                        AVAILABLE_COLOR
                );

        storagePanel.add(
                equipmentStatus
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

            JTextArea bankStatus =
                    createWrappedText(
                            "\u2713 Main bank: VERIFIED"
                                    + " ("
                                    + storageSnapshot
                                    .getBankDistinctItemCount()
                                    + " item types)"
                                    + " - synced "
                                    + syncTime,
                            AVAILABLE_COLOR
                    );

            storagePanel.add(
                    bankStatus
            );

            storagePanel.add(
                    Box.createVerticalStrut(4)
            );

            JTextArea routeStatus =
                    createWrappedText(
                            "Verified storage requirements satisfied for this login session.",
                            AVAILABLE_COLOR
                    );

            storagePanel.add(
                    routeStatus
            );
        }
        else
        {
            JTextArea bankStatus =
                    createWrappedText(
                            "\u26A0 Main bank: SYNC REQUIRED",
                            WARNING_COLOR
                    );

            storagePanel.add(
                    bankStatus
            );

            storagePanel.add(
                    Box.createVerticalStrut(4)
            );

            JTextArea instruction =
                    createWrappedText(
                            "Open any normal bank once. Farming Navigator "
                                    + "will read the full bank container and mark "
                                    + "it verified for this login session.",
                            WARNING_COLOR
                    );

            storagePanel.add(
                    instruction
            );

            storagePanel.add(
                    Box.createVerticalStrut(4)
            );

            JTextArea routeStatus =
                    createWrappedText(
                            "Verified route planning will remain locked until the bank is synchronized.",
                            LOCKED_COLOR
                    );

            storagePanel.add(
                    routeStatus
            );
        }

        patchListPanel.add(
                storagePanel
        );
    }

    private JPanel createPatchRow(
            HerbPatchStatus status,
            List<TransportOption> transportOptions
    )
    {
        HerbPatchDefinition patch =
                status.getPatch();

        JPanel row =
                new JPanel();

        row.setLayout(
                new BoxLayout(
                        row,
                        BoxLayout.Y_AXIS
                )
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

        row.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        row.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );

        JPanel titleRow =
                new JPanel(
                        new BorderLayout(
                                8,
                                0
                        )
                );

        titleRow.setOpaque(
                false
        );

        titleRow.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        titleRow.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        24
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

        JLabel patchNameLabel =
                new JLabel(
                        patch.getDisplayName()
                );

        patchNameLabel.setForeground(
                Color.WHITE
        );

        titleRow.add(
                stateLabel,
                BorderLayout.WEST
        );

        titleRow.add(
                patchNameLabel,
                BorderLayout.CENTER
        );

        row.add(
                titleRow
        );

        if (
                patch.isAlwaysDiseaseFree()
        )
        {
            row.add(
                    createWrappedText(
                            "Disease-free patch",
                            DISEASE_FREE_COLOR
                    )
            );
        }

        if (status.isUnlocked())
        {
            row.add(
                    createWrappedText(
                            "Available",
                            AVAILABLE_COLOR
                    )
            );

            appendTransportOptions(
                    row,
                    transportOptions
            );
        }
        else
        {
            StringBuilder requirements =
                    new StringBuilder(
                            "Requires: "
                    );

            for (
                    int index = 0;
                    index
                            <
                            status
                                    .getMissingRequirements()
                                    .size();
                    index++
            )
            {
                if (index > 0)
                {
                    requirements.append(
                            ", "
                    );
                }

                requirements.append(
                        status
                                .getMissingRequirements()
                                .get(index)
                );
            }

            row.add(
                    createWrappedText(
                            requirements.toString(),
                            LOCKED_COLOR
                    )
            );
        }

        return row;
    }

    private void appendTransportOptions(
            JPanel row,
            List<TransportOption> transportOptions
    )
    {
        if (transportOptions.isEmpty())
        {
            return;
        }

        row.add(
                Box.createVerticalStrut(4)
        );

        row.add(
                createWrappedText(
                        "Baseline travel:",
                        MUTED_COLOR
                )
        );

        int numberToShow =
                Math.min(
                        2,
                        transportOptions.size()
                );

        for (
                int index = 0;
                index < numberToShow;
                index++
        )
        {
            TransportOption option =
                    transportOptions.get(
                            index
                    );

            row.add(
                    createWrappedText(
                            (index + 1)
                                    + ". "
                                    + option
                                    .getDisplayName(),
                            Color.WHITE
                    )
            );
        }
    }

    private JTextArea createWrappedText(
            String text,
            Color color
    )
    {
        JTextArea textArea =
                new JTextArea(
                        text
                );

        textArea.setEditable(
                false
        );

        textArea.setFocusable(
                false
        );

        textArea.setOpaque(
                false
        );

        textArea.setLineWrap(
                true
        );

        textArea.setWrapStyleWord(
                true
        );

        textArea.setForeground(
                color
        );

        textArea.setFont(
                UIManager.getFont(
                        "Label.font"
                )
        );

        textArea.setBorder(
                null
        );

        textArea.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        textArea.setMinimumSize(
                new Dimension(
                        0,
                        0
                )
        );

        textArea.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );

        return textArea;
    }

    private String formatSyncTime(
            Instant instant
    )
    {
        if (instant == null)
        {
            return "this session";
        }

        return SYNC_TIME_FORMAT.format(
                instant
        );
    }

    private void resetScrollPosition()
    {
        patchScrollPane
                .getVerticalScrollBar()
                .setValue(0);
    }

    private void refreshPatchPanel()
    {
        patchListPanel.revalidate();
        patchListPanel.repaint();

        patchScrollPane.revalidate();
        patchScrollPane.repaint();
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