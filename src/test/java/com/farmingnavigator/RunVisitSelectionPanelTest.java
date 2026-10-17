package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.routing.run.RunVisitSelection;
import com.farmingnavigator.storage.StorageSnapshot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Offscreen sidebar rendering; never starts the game client. */
public class RunVisitSelectionPanelTest
{
    @Test public void filterNoticeAndFallbackPreparationUseTheSelectedHistoricalMode() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.now();
            FarmingNavigatorPanel root = new FarmingNavigatorPanel(() -> {}, null);
            root.setTreeObservations(Map.of(TreePatchDefinition.VARROCK_TREE, new TreePatchObservation(
                    new TreePatchStateDecoder().decode(TreeFamily.TREE, 12), now.minusSeconds(86400), FarmingGrowthRate.NORMAL)));
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(), Map.of(), Map.of());
            StorageSnapshot storage = new StorageSnapshot(Map.of(), Map.of(), Map.of(), false, null);
            root.setAccountState(account, storage, List.of(), Map.of(), new FarmingNavigatorConfig() {
                @Override public RunVisitSelection runVisitSelection() { return RunVisitSelection.OBSERVED_MATURE; }
            }, Map.of());
            JLabel heading = heading(root); assertNotNull(heading);
            Container card = heading.getParent();
            String labels = labels(card);
            assertTrue(labels.contains("Last observed mature"));
            assertTrue(labels.contains("historical observations"));
            assertTrue(labels.contains("Mature plants may have no produce"));
            assertTrue(labels(root).contains("Oak - 1 stops"));
            render((JPanel) card);
            root.setAccountState(account, storage, List.of(), Map.of(), new FarmingNavigatorConfig(){}, Map.of());
            assertNull(heading(root));
            assertTrue(labels(root).contains("Oak - 6 stops"));
            root.setAccountState(AccountState.loggedOut(), storage, List.of(), Map.of(), new FarmingNavigatorConfig(){}, Map.of());
            assertNull(heading(root));
        });
    }
    private JLabel heading(Component component)
    {
        if (component instanceof JLabel && "Run visit selection".equals(((JLabel) component).getText())) { return (JLabel) component; }
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents())
        { JLabel result = heading(child); if (result != null) { return result; } } }
        return null;
    }
    private String labels(Component component)
    {
        StringBuilder result = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() + " " : "");
        if (component instanceof Container) { for (Component child : ((Container) component).getComponents()) { result.append(labels(child)); } }
        return result.toString();
    }
    private void render(JPanel panel)
    {
        panel.setSize(225, panel.getPreferredSize().height); layout(panel);
        BufferedImage image = new BufferedImage(225, panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
        File directory = new File("build/verification"); directory.mkdirs();
        try { ImageIO.write(image, "png", new File(directory, "run-visit-selection.png")); }
        catch (Exception ex) { throw new AssertionError(ex); }
    }
    private void layout(Container parent)
    { parent.doLayout(); for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } } }
}
