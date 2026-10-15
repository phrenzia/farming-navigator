package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.state.HerbPatchRegion;
import com.farmingnavigator.guided.HerbPatchDestination;
import java.util.Collections;
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

/** Spawn/despawn cache. Initialization scans only the nearby farming area once. */
@Singleton
public final class HerbSceneTracker
{
    private final Client client;
    private final Map<GameObject, HerbPatchDefinition> herbs = new IdentityHashMap<>();
    private final Set<NPC> leprechauns = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<GameObject, HerbPatchDefinition> herbView = Collections.unmodifiableMap(herbs);
    private final Set<NPC> leprechaunView = Collections.unmodifiableSet(leprechauns);

    @Inject
    public HerbSceneTracker(Client client)
    {
        this.client = client;
    }

    public void spawned(GameObject object)
    {
        HerbPatchRegion region = HerbPatchRegion.forLocation(object.getWorldLocation());
        if (region == null || !HerbPatchDestination.isNear(region.getPatch(), object.getWorldLocation()))
        {
            return;
        }
        ObjectComposition base = client.getObjectDefinition(object.getId());
        ObjectComposition transformed = base.getImpostorIds() == null ? base : base.getImpostor();
        HerbPatchDefinition patch = HerbSceneTargetPolicy.herbPatch(object.getId(), base.getVarbitId(),
                transformed == null ? null : transformed.getName(), object.getWorldLocation());
        if (patch != null)
        {
            herbs.put(object, patch);
        }
    }

    public void despawned(GameObject object) { herbs.remove(object); }
    public void despawned(NPC npc) { leprechauns.remove(npc); }
    public void spawned(NPC npc)
    {
        if (HerbSceneTargetPolicy.isToolLeprechaun(npc.getId())) { leprechauns.add(npc); }
    }
    public Map<GameObject, HerbPatchDefinition> getHerbs() { return herbView; }
    public Set<NPC> getLeprechauns() { return leprechaunView; }

    public void clear()
    {
        herbs.clear();
        leprechauns.clear();
    }

    public void initializeNearby()
    {
        clear();
        WorldView view = client.getTopLevelWorldView();
        if (client.getGameState() != GameState.LOGGED_IN || view == null || view.isInstance() || client.getLocalPlayer() == null)
        {
            return;
        }
        for (NPC npc : view.npcs()) { spawned(npc); }
        HerbPatchRegion region = HerbPatchRegion.forLocation(client.getLocalPlayer().getWorldLocation());
        if (region == null) { return; }
        LocalPoint anchor = LocalPoint.fromWorld(view, HerbPatchDestination.valueOf(region.getPatch().name()).getLocation());
        if (anchor == null) { return; }
        Tile[][] tiles = view.getScene().getTiles()[0];
        for (int x = Math.max(0, anchor.getSceneX() - 12); x <= Math.min(tiles.length - 1, anchor.getSceneX() + 12); x++)
        {
            for (int y = Math.max(0, anchor.getSceneY() - 12); y <= Math.min(tiles[x].length - 1, anchor.getSceneY() + 12); y++)
            {
                Tile tile = tiles[x][y];
                if (tile == null) { continue; }
                for (GameObject object : tile.getGameObjects())
                {
                    if (object != null) { spawned(object); }
                }
            }
        }
    }
}
