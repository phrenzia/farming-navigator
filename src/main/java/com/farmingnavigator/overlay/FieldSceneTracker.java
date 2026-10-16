package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchRegion;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/** Event cache with a one-off nearby-area initialization, never a per-tick or per-frame scene scan. */
@Singleton
public final class FieldSceneTracker
{
    private final Client client;
    private final Map<GameObject, FieldPatchDefinition> plants = new IdentityHashMap<>();
    private final Set<NPC> farmers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<GameObject, FieldPatchDefinition> plantView = Collections.unmodifiableMap(plants);
    private final Set<NPC> farmerView = Collections.unmodifiableSet(farmers);
    @Inject public FieldSceneTracker(Client client) { this.client = client; }
    public void spawned(GameObject object)
    {
        FieldPatchRegion region = FieldPatchRegion.forLocation(object.getWorldLocation());
        if (region == null || region.getPatches().stream().noneMatch(patch -> patch.isNear(object.getWorldLocation()))) { return; }
        ObjectComposition base = client.getObjectDefinition(object.getId());
        FieldPatchDefinition patch = FieldSceneTargetPolicy.fieldPatch(object.getId(), base.getVarbitId(), object.getWorldLocation());
        if (patch != null) { plants.put(object, patch); }
    }
    public void despawned(GameObject object) { plants.remove(object); }
    public void spawned(NPC npc) { if (FieldSceneTargetPolicy.isKnownFarmer(npc.getId())) { farmers.add(npc); } }
    public void despawned(NPC npc) { farmers.remove(npc); }
    public Map<GameObject, FieldPatchDefinition> getPlants() { return plantView; }
    public Set<NPC> getFarmers() { return farmerView; }
    public void clear() { plants.clear(); farmers.clear(); }
    public void initializeNearby()
    {
        clear();
        WorldView view = client.getTopLevelWorldView();
        if (client.getGameState() != GameState.LOGGED_IN || view == null || view.isInstance() || client.getLocalPlayer() == null) { return; }
        for (NPC npc : view.npcs()) { spawned(npc); }
        FieldPatchRegion region = FieldPatchRegion.forLocation(client.getLocalPlayer().getWorldLocation());
        if (region == null) { return; }
        Set<WorldPoint> anchors = new HashSet<>();
        for (FieldPatchDefinition patch : region.getPatches()) { anchors.add(patch.getLocation()); }
        Tile[][] tiles = view.getScene().getTiles()[0];
        for (WorldPoint destination : anchors)
        {
            LocalPoint anchor = LocalPoint.fromWorld(view, destination);
            if (anchor == null) { continue; }
            for (int x = Math.max(0, anchor.getSceneX() - 12); x <= Math.min(tiles.length - 1, anchor.getSceneX() + 12); x++)
            {
                for (int y = Math.max(0, anchor.getSceneY() - 12); y <= Math.min(tiles[x].length - 1, anchor.getSceneY() + 12); y++)
                {
                    Tile tile = tiles[x][y];
                    if (tile == null) { continue; }
                    for (GameObject object : tile.getGameObjects()) { if (object != null) { spawned(object); } }
                }
            }
        }
    }
}
