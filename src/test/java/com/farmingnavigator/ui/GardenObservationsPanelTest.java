package com.farmingnavigator.ui;

import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenObservationsPanelTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private GardenPatchObservation observation(GardenFamily family, int raw, Instant at)
    {
        return new GardenPatchObservation(new GardenPatchStateDecoder().decode(family, raw), at, FarmingGrowthRate.NORMAL);
    }
    @Test public void textSeparatesMatureProduceFromConditionalFirstGrowth()
    {
        GardenPatchObservation growing = observation(GardenFamily.BUSH, 5, now);
        assertEquals("Estimated health check in 80-100 min if healthy.", GardenObservationText.timer(growing, now));
        assertTrue(GardenObservationText.timer(growing, now.plusSeconds(6000)).contains("visit to confirm"));
        GardenPatchObservation mature = observation(GardenFamily.CACTUS, 15, now);
        assertTrue(GardenObservationText.describe(mature, ZoneId.of("UTC")).contains("Health checked; inspect for produce"));
        assertEquals("", GardenObservationText.timer(mature, now));
        assertTrue(GardenObservationText.describe(null, ZoneId.of("UTC")).contains("Data required"));
    }
    @Test public void rendersBothFamiliesAndExpansionWithoutAClient() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean expanded = new AtomicBoolean();
            GardenObservationsPanel panel = new GardenObservationsPanel(Map.of(
                    GardenPatchDefinition.RIMMINGTON_BUSH, observation(GardenFamily.BUSH, 250, now),
                    GardenPatchDefinition.FARMING_GUILD_BUSH, observation(GardenFamily.BUSH, 70, now),
                    GardenPatchDefinition.AL_KHARID_CACTUS, observation(GardenFamily.CACTUS, 32, now)), now, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick(); assertTrue(expanded.get());
            assertTrue(labels(panel).contains("Observed patches: 3 / 7"));
            assertTrue(labels(panel).contains("health-check ready: 1; diseased or dead: 1"));
            assertTrue(labels(panel).contains("Bushes")); assertTrue(labels(panel).contains("Cacti"));
            panel.setSize(225, 2200); layout(panel);
            BufferedImage image = new BufferedImage(225, 2200, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "garden-observations.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
            toggle.doClick(); assertFalse(expanded.get());
        });
    }
    @Test public void futureHistoryDoesNotCountAsReady() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            GardenObservationsPanel panel = new GardenObservationsPanel(Map.of(GardenPatchDefinition.FARMING_GUILD_CACTUS,
                    observation(GardenFamily.CACTUS, 58, now.plusSeconds(1))), now, true, value -> {});
            assertTrue(labels(panel).contains("Observed patches: 0 / 7"));
            assertFalse(labels(panel).contains("health-check ready: 1"));
        });
    }
    private String labels(Component component)
    {
        StringBuilder text = new StringBuilder(component instanceof JLabel ? ((JLabel) component).getText() : "");
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents()) { text.append(labels(child)); }
        }
        return text.toString();
    }
    private void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
