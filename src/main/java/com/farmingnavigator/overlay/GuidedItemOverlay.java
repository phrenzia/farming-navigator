package com.farmingnavigator.overlay;

import com.farmingnavigator.FarmingNavigatorConfig;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

abstract class GuidedItemOverlay extends WidgetItemOverlay
{
    private static final Color FILL = new Color(70, 220, 130, 55);
    private static final Color OUTLINE = new Color(70, 220, 130);
    private final FarmingNavigatorConfig config;
    private final GuidedRunOverlayController controller;
    private final boolean bank;

    GuidedItemOverlay(FarmingNavigatorConfig config, GuidedRunOverlayController controller, boolean bank)
    {
        this.config = config;
        this.controller = controller;
        this.bank = bank;
        if (bank)
        {
            showOnBank();
        }
        else
        {
            showOnInventory();
            showOnEquipment();
        }
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem item)
    {
        if (!config.guidedRunOverlays() || item.getQuantity() <= 0
                || !(bank ? controller.getTargets().getBankItems() : controller.getTargets().getInventoryItems()).contains(itemId))
        {
            return;
        }
        Rectangle bounds = item.getCanvasBounds();
        Color previous = graphics.getColor();
        graphics.setColor(FILL);
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        graphics.setColor(OUTLINE);
        graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        graphics.setColor(previous);
    }
}
