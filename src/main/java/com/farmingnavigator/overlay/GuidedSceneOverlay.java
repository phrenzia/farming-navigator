package com.farmingnavigator.overlay;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.guided.HerbPatchDestination;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public final class GuidedSceneOverlay extends Overlay
{
    private static final Color OUTLINE = new Color(70, 220, 130);
    private static final Color FILL = new Color(70, 220, 130, 35);
    private final Client client;
    private final FarmingNavigatorConfig config;
    private final GuidedRunOverlayController controller;
    private final HerbSceneTracker scene;
    private final TreeSceneTracker treeScene;
    private final GardenSceneTracker gardenScene;

    @Inject
    public GuidedSceneOverlay(Client client, FarmingNavigatorConfig config,
            GuidedRunOverlayController controller, HerbSceneTracker scene, TreeSceneTracker treeScene, GardenSceneTracker gardenScene)
    {
        this.client = client;
        this.config = config;
        this.controller = controller;
        this.scene = scene;
        this.treeScene = treeScene;
        this.gardenScene = gardenScene;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        GuidedRunTargets targets = controller.getTargets();
        WorldView view = client.getTopLevelWorldView();
        if (!config.guidedRunOverlays() || view == null || view.isInstance()
                || targets.getScenePatch() == null && targets.getTreeScenePatch() == null && targets.getGardenScenePatch() == null)
        {
            return null;
        }
        if (targets.getGardenScenePatch() != null)
        {
            if (targets.isGardenFarmer())
            {
                for (NPC npc : gardenScene.getFarmers())
                {
                    if (GardenSceneTargetPolicy.isFarmerFor(targets.getGardenScenePatch(), npc.getId(), npc.getWorldLocation())) { draw(graphics, npc.getCanvasTilePoly()); }
                }
            }
            else
            {
                for (java.util.Map.Entry<GameObject, com.farmingnavigator.farming.garden.GardenPatchDefinition> entry : gardenScene.getPlants().entrySet())
                {
                    if (entry.getValue() == targets.getGardenScenePatch()) { draw(graphics, entry.getKey().getCanvasTilePoly()); }
                }
            }
            return null;
        }
        if (targets.getTreeScenePatch() != null)
        {
            if (targets.isTreeFarmer())
            {
                for (NPC npc : treeScene.getFarmers())
                {
                    if (TreeSceneTargetPolicy.isFarmerFor(targets.getTreeScenePatch(), npc.getId(), npc.getWorldLocation()))
                    {
                        draw(graphics, npc.getCanvasTilePoly());
                    }
                }
            }
            else
            {
                for (java.util.Map.Entry<GameObject, com.farmingnavigator.farming.tree.TreePatchDefinition> entry : treeScene.getTrees().entrySet())
                {
                    if (entry.getValue() == targets.getTreeScenePatch()) { draw(graphics, entry.getKey().getCanvasTilePoly()); }
                }
            }
            return null;
        }
        if (targets.isLeprechaun())
        {
            for (NPC npc : scene.getLeprechauns())
            {
                if (npc.getWorldLocation().getPlane() == 0 && HerbPatchDestination.valueOf(targets.getScenePatch().name())
                        .getLocation().distanceTo2D(npc.getWorldLocation()) <= 25)
                {
                    draw(graphics, npc.getCanvasTilePoly());
                }
            }
        }
        else
        {
            for (java.util.Map.Entry<GameObject, com.farmingnavigator.farming.HerbPatchDefinition> entry : scene.getHerbs().entrySet())
            {
                if (entry.getValue() == targets.getScenePatch()) { draw(graphics, entry.getKey().getCanvasTilePoly()); }
            }
        }
        return null;
    }

    private void draw(Graphics2D graphics, Polygon polygon)
    {
        if (polygon == null) { return; }
        Color previous = graphics.getColor();
        graphics.setColor(FILL);
        graphics.fillPolygon(polygon);
        graphics.setColor(OUTLINE);
        graphics.drawPolygon(polygon);
        graphics.setColor(previous);
    }
}
