package com.farmingnavigator.overlay;

import com.farmingnavigator.guided.GuidedRunSnapshot;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.run.HerbRunPlan;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

@Singleton
public final class GuidedRunOverlayController
{
    private final WorldMapPointManager worldMap;
    private final BufferedImage markerImage = createMarkerImage();
    private volatile GuidedRunTargets targets = GuidedRunTargets.empty();
    private WorldMapPoint marker;

    @Inject
    public GuidedRunOverlayController(WorldMapPointManager worldMap)
    {
        this.worldMap = worldMap;
    }

    public GuidedRunTargets getTargets()
    {
        return targets;
    }

    public void update(GuidedRunSnapshot session, HerbRunPlan plan, TravelCapabilitySnapshot travel, boolean enabled)
    {
        applyTargets(enabled ? GuidedRunTargets.from(session, plan, travel) : GuidedRunTargets.empty());
    }

    public void updateTree(com.farmingnavigator.guided.tree.TreeRunSnapshot session,
            com.farmingnavigator.routing.run.TreeRunPlan plan, TravelCapabilitySnapshot travel, boolean enabled)
    {
        applyTargets(enabled ? GuidedRunTargets.fromTree(session, plan, travel) : GuidedRunTargets.empty());
    }

    private void applyTargets(GuidedRunTargets next)
    {
        targets = next;
        if (marker != null && !marker.getWorldPoint().equals(targets.getDestination()))
        {
            worldMap.remove(marker);
            marker = null;
        }
        if (marker == null && targets.getDestination() != null)
        {
            marker = new WorldMapPoint(targets.getDestination(), markerImage);
            marker.setTooltip("Guided farming run: " + targets.getDestinationName());
            marker.setName("Farming Navigator destination");
            marker.setSnapToEdge(true);
            marker.setJumpOnClick(false);
            worldMap.add(marker);
        }
    }

    public void clear()
    {
        targets = GuidedRunTargets.empty();
        if (marker != null)
        {
            worldMap.remove(marker);
            marker = null;
        }
    }

    private BufferedImage createMarkerImage()
    {
        BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(60, 210, 120));
        graphics.fillOval(1, 1, 15, 15);
        graphics.setColor(Color.BLACK);
        graphics.drawOval(1, 1, 15, 15);
        graphics.drawLine(8, 4, 8, 13);
        graphics.drawLine(4, 8, 13, 8);
        graphics.dispose();
        return image;
    }
}
