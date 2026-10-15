package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import com.farmingnavigator.guided.GuidedRunCommand;
import com.farmingnavigator.guided.GuidedRunSession;
import com.farmingnavigator.guided.HerbPatchDestination;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbRunPlanner;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GuidedRunPanelTest
{
    @Test
    public void rendersPlantingStepAndButtonsEmitChecklistCommandsOnly() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Instant now = Instant.parse("2026-10-15T12:00:00Z");
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
            HerbRunPlan plan = new HerbRunPlanner().plan(account, Collections.singletonList(
                    new HerbPatchStatus(HerbPatchDefinition.FALADOR, Collections.emptyList())),
                    EnumSet.of(HerbPatchDefinition.FALADOR), HerbRunCrop.RANARR, HerbRunCompost.NONE,
                    Collections.emptyMap(), Collections.emptyMap(), null, now);
            GuidedRunSession session = new GuidedRunSession();
            session.start(plan, HerbRunCompost.NONE, now);
            session.confirm(now.plusSeconds(1));
            session.observe(HerbPatchDestination.FALADOR.getLocation(), Collections.singletonMap(HerbPatchDefinition.FALADOR,
                    new HerbPatchObservation(new HerbPatchStateDecoder().decode(0), now.plusSeconds(2))), now.plusSeconds(2));
            List<GuidedRunCommand> commands = new ArrayList<>();
            GuidedRunPanel panel = new GuidedRunPanel(session.snapshot(), true, commands::add);
            panel.setSize(225, 600);
            layout(panel);
            BufferedImage image = new BufferedImage(225, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
            File directory = new File("build/verification");
            directory.mkdirs();
            try
            {
                ImageIO.write(image, "png", new File(directory, "guided-run.png"));
            }
            catch (Exception error)
            {
                throw new AssertionError(error);
            }
            JButton manual = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton
                    && ((JButton) c).getText().equals("Complete stop manually")).findFirst().get();
            manual.doClick();
            manual.doClick();
            assertEquals(Collections.singletonList(GuidedRunCommand.MARK_COMPLETE), commands);
        });
    }

    private void layout(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents())
        {
            if (child instanceof Container) { layout((Container) child); }
        }
    }
}
