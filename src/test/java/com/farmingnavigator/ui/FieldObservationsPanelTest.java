package com.farmingnavigator.ui;

import com.farmingnavigator.farming.field.*;
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

public class FieldObservationsPanelTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private FieldPatchObservation observation(FieldFamily family, int raw, Instant at)
    {
        return new FieldPatchObservation(new FieldPatchStateDecoder().decode(family, raw), at, FarmingGrowthRate.NORMAL);
    }
    @Test public void wateredHistoryExplainsThatTheObservedCycleDoesNotProtectLaterCycles()
    {
        assertTrue(FieldObservationText.describe(observation(FieldFamily.ALLOTMENT,70,now),ZoneId.of("UTC")).contains("Watered during the observed growth cycle"));
        assertFalse(FieldObservationText.describe(observation(FieldFamily.ALLOTMENT,6,now),ZoneId.of("UTC")).contains("Watered during"));
    }
    @Test public void textSeparatesObservedHarvestFromConditionalGrowth()
    {
        FieldPatchObservation growing = observation(FieldFamily.ALLOTMENT, 6, now);
        assertEquals("Estimated harvest in 30-40 min if healthy.", FieldObservationText.timer(growing, now));
        assertTrue(FieldObservationText.timer(growing, now.plusSeconds(6000)).contains("visit to confirm"));
        FieldPatchObservation mature = observation(FieldFamily.FLOWER, 12, now);
        assertTrue(FieldObservationText.describe(mature, ZoneId.of("UTC")).contains("Harvestable; quantity unknown"));
        assertEquals("", FieldObservationText.timer(mature, now));
        assertTrue(FieldObservationText.describe(null, ZoneId.of("UTC")).contains("Data required"));
    }
    @Test public void rendersBothFamiliesAndExpansionWithoutAClient() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean expanded = new AtomicBoolean();
            FieldObservationsPanel panel = new FieldObservationsPanel(Map.of(
                    FieldPatchDefinition.FALADOR_NORTH, observation(FieldFamily.ALLOTMENT, 10, now),
                    FieldPatchDefinition.FARMING_GUILD_NORTH, observation(FieldFamily.ALLOTMENT, 135, now),
                    FieldPatchDefinition.PRIFDDINAS_FLOWER, observation(FieldFamily.FLOWER, 37, now)), now, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            toggle.doClick(); assertTrue(expanded.get());
            assertTrue(labels(panel).contains("Observed patches: 3 / 48"));
            assertTrue(labels(panel).contains("harvestable: 1; diseased or dead: 1"));
            assertTrue(labels(panel).contains("Allotments")); assertTrue(labels(panel).contains("Flowers"));
            panel.setSize(225, 6800); layout(panel);
            BufferedImage image = new BufferedImage(225, 6800, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
            File directory = new File("build/verification"); directory.mkdirs();
            try { ImageIO.write(image, "png", new File(directory, "field-observations.png")); }
            catch (Exception ex) { throw new AssertionError(ex); }
            toggle.doClick(); assertFalse(expanded.get());
        });
    }
    @Test public void futureHistoryDoesNotCountAsReady() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            FieldObservationsPanel panel = new FieldObservationsPanel(Map.of(FieldPatchDefinition.FARMING_GUILD_FLOWER,
                    observation(FieldFamily.FLOWER, 41, now.plusSeconds(1))), now, true, value -> {});
            assertTrue(labels(panel).contains("Observed patches: 0 / 48"));
            assertFalse(labels(panel).contains("harvestable: 1"));
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
