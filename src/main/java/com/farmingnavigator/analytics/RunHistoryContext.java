package com.farmingnavigator.analytics;

import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;

/** Immutable setup captured at checklist start. It describes intent, never actual yield or completed actions. */
public final class RunHistoryContext
{
    public static final RunHistoryContext UNKNOWN=new RunHistoryContext(null,"-","",0);
    private final RunHistoryStore.Kind kind;
    private final String cropKey,cropLabel;
    private final int plannedStops;
    private RunHistoryContext(RunHistoryStore.Kind kind,String key,String label,int stops)
    {this.kind=kind;cropKey=key;cropLabel=label;plannedStops=stops;}
    public static RunHistoryContext of(RunHistoryStore.Kind kind,String cropKey,int plannedStops)
    {
        if(kind==null || cropKey==null){throw new IllegalArgumentException("Run kind and crop are required.");}
        String label;int maximum;
        switch(kind)
        {
            case HERB:label=HerbRunCrop.valueOf(cropKey).getDisplayName();maximum=HerbPatchDefinition.values().length;break;
            case TREE:label=TreeRunCrop.valueOf(cropKey).toString();maximum=TreePatchDefinition.values().length;break;
            case GARDEN:label=GardenRunCrop.valueOf(cropKey).toString();maximum=GardenPatchDefinition.values().length;break;
            case FIELD:label=FieldRunCrop.valueOf(cropKey).toString();maximum=FieldPatchDefinition.values().length;break;
            default:throw new IllegalArgumentException("Unknown run kind.");
        }
        if(plannedStops<1 || plannedStops>maximum){throw new IllegalArgumentException("Planned stop count is outside this run family's bounds.");}
        return new RunHistoryContext(kind,cropKey,label,plannedStops);
    }
    public boolean isKnown(){return kind!=null;}
    public boolean isValidFor(RunHistoryStore.Kind expected){return !isKnown() || kind==expected;}
    public String getCropKey(){return cropKey;}
    public String getCropLabel(){return cropLabel;}
    public int getPlannedStops(){return plannedStops;}
}
