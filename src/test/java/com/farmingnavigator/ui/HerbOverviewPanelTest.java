package com.farmingnavigator.ui;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerbOverviewPanelTest
{
    @Test public void rendersHistoricalAndEstimatedSummaryAndExpansionOffline() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.parse("2026-10-15T12:00:00Z");
            HerbOverview overview = HerbOverview.create(Arrays.asList(
                    new HerbPatchStatus(HerbPatchDefinition.FALADOR, Collections.emptyList()),
                    new HerbPatchStatus(HerbPatchDefinition.CATHERBY, Collections.emptyList())), EnumSet.allOf(HerbPatchDefinition.class),
                    Map.of(HerbPatchDefinition.FALADOR, new HerbPatchObservation(new HerbPatchStateDecoder().decode(128), now),
                            HerbPatchDefinition.CATHERBY, new HerbPatchObservation(new HerbPatchStateDecoder().decode(4),
                                    now.minusSeconds(3600), FarmingGrowthRate.NORMAL)), now);
            AtomicBoolean expanded = new AtomicBoolean();
            HerbOverviewPanel panel = new HerbOverviewPanel(overview, now, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick();
            assertTrue(expanded.get());
            assertTrue(labels(panel).contains("Last observed diseased: 1"));
            assertTrue(labels(panel).contains("Possibly ready if healthy (estimate): 1"));
            assertTrue(labels(panel).contains("Observed 60 min ago"));
            assertFalse(labels(panel).contains("Last observed ready: 1"));
            panel.setSize(225, 900);
            layout(panel);
            BufferedImage image = new BufferedImage(225, 900, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
            File directory = new File("build/verification");
            directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "herb-overview.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
            toggle.doClick();
            assertFalse(expanded.get());
        });
    }

    private String labels(Component component)
    {
        StringBuilder result = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents()) { result.append(labels(child)); }
        }
        return result.toString();
    }

    private void layout(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
