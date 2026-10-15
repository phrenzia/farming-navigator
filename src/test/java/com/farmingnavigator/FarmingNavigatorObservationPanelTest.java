package com.farmingnavigator;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import com.farmingnavigator.storage.StorageSnapshot;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.Collections;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Renders Swing components offscreen; does not start RuneLite or any game client. */
public class FarmingNavigatorObservationPanelTest
{
    @Test
    public void patchObservationAppearsBeforeBankVerification() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FarmingNavigatorPanel panel = new FarmingNavigatorPanel(() -> {}, null);
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
            StorageSnapshot storage = new StorageSnapshot(Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), false, null);
            Map<HerbPatchDefinition, HerbPatchObservation> observations = new EnumMap<>(HerbPatchDefinition.class);
            observations.put(HerbPatchDefinition.FALADOR, new HerbPatchObservation(
                    new HerbPatchStateDecoder().decode(140), Instant.parse("2026-10-15T12:00:00Z")));
            observations.put(HerbPatchDefinition.CATHERBY, new HerbPatchObservation(
                    new HerbPatchStateDecoder().decode(32), Instant.now().minusSeconds(1), FarmingGrowthRate.NORMAL));
            panel.setAccountState(account, storage, Arrays.asList(
                    new HerbPatchStatus(HerbPatchDefinition.FALADOR, Collections.emptyList()),
                    new HerbPatchStatus(HerbPatchDefinition.CATHERBY, Collections.emptyList())),
                    Collections.emptyMap(), new FarmingNavigatorConfig() {}, observations);
            assertTrue(containsLabel(panel, "Ranarr - Diseased"));
            assertTrue(containsLabel(panel, "bank sync required"));
            assertTrue(containsLabel(panel, "Estimated harvest in 60-80 min if healthy."));
            assertFalse(containsLabel(panel, "visit this patch to observe it"));
            panel.setSize(240, 1300);
            layout(panel);
            BufferedImage image = new BufferedImage(240, 1300, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
            File directory = new File("build/verification");
            directory.mkdirs();
            try
            {
                ImageIO.write(image, "png", new File(directory, "herb-patch-panel.png"));
            }
            catch (Exception error)
            {
                throw new AssertionError(error);
            }
        });
    }

    private boolean containsLabel(Component component, String text)
    {
        StringBuilder labels = new StringBuilder();
        collectLabels(component, labels);
        return labels.toString().contains(text);
    }

    private void collectLabels(Component component, StringBuilder labels)
    {
        if (component instanceof JLabel)
        {
            labels.append(((JLabel) component).getText()).append(' ');
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                collectLabels(child, labels);
            }
        }
    }

    private void layout(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents())
        {
            if (child instanceof Container)
            {
                layout((Container) child);
            }
        }
    }
}
