package com.farmingnavigator.ui;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbRunPlanner;
import com.farmingnavigator.storage.StorageSnapshot;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HerbRunPlanPanelTest
{
    @Test
    public void rendersAndExpandsPlanWithoutStartingGameClient() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
            StorageSnapshot storage = new StorageSnapshot(Collections.singletonMap(ItemID.RANARR_SEED, 1L),
                    Collections.emptyMap(), Collections.singletonMap(ItemID.RANARR_SEED, 1L), true, Instant.now());
            HerbRunPlan plan = new HerbRunPlanner().plan(account, Arrays.asList(
                    new HerbPatchStatus(HerbPatchDefinition.FALADOR, Collections.emptyList()),
                    new HerbPatchStatus(HerbPatchDefinition.CATHERBY, Collections.emptyList())),
                    EnumSet.allOf(HerbPatchDefinition.class), HerbRunCrop.RANARR, HerbRunCompost.ULTRACOMPOST,
                    Collections.emptyMap(), Collections.emptyMap(), storage, Instant.now());
            AtomicBoolean expanded = new AtomicBoolean();
            HerbRunPlanPanel panel = new HerbRunPlanPanel(plan, false, expanded::set);
            JButton toggle = (JButton) Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).findFirst().get();
            assertFalse(expanded.get());
            toggle.doClick();
            assertTrue(expanded.get());
            panel.setSize(225, 2000);
            layout(panel);
            BufferedImage image = new BufferedImage(225, 2000, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            panel.printAll(graphics);
            graphics.dispose();
            File directory = new File("build/verification");
            directory.mkdirs();
            try
            {
                ImageIO.write(image, "png", new File(directory, "herb-run-plan.png"));
            }
            catch (Exception error)
            {
                throw new AssertionError(error);
            }
            toggle.doClick();
            assertFalse(expanded.get());
        });
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
