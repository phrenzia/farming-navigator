package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.capability.SpellbookType;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import net.runelite.api.Quest;

/** Farming-action runes must be combined with travel runes, not checked in isolation. */
public final class HerbRunCareResources
{
    // https://oldschool.runescape.wiki/w/Cure_Plant : Lunar, level 66, eight earth and one astral.
    public static final RuneCost CURE_PLANT_COST = RuneCost.builder().add(RuneKind.EARTH, 8).add(RuneKind.ASTRAL, 1).build();
    private final HerbRunCure cure;
    private final RuneCost runes;
    private final List<RequirementResult> checks;

    private HerbRunCareResources(HerbRunCure cure, RuneCost runes, List<RequirementResult> checks)
    {
        this.cure = cure;
        this.runes = runes;
        this.checks = Collections.unmodifiableList(checks);
    }

    public static HerbRunCareResources evaluate(HerbRunPlan plan, HerbRunCure cure, RequirementContext context)
    {
        return evaluate(plan, cure, HerbRunCompost.NONE, context);
    }

    public static HerbRunCareResources evaluate(HerbRunPlan plan, HerbRunCure cure, HerbRunCompost compost,
            RequirementContext context)
    {
        if (cure == null) { cure = HerbRunCure.PLANT_CURE; }
        int cures = (int) plan.getStops().stream().filter(stop -> !stop.isReplant()).count();
        RuneCost.Builder runes = RuneCost.builder();
        List<RequirementResult> checks = new ArrayList<>();
        if (cure == HerbRunCure.CURE_PLANT && cures > 0)
        {
            CURE_PLANT_COST.getQuantities().forEach((kind, quantity) -> runes.add(kind, quantity * cures));
            checks.add(new AllOfRequirement("Cure Plant access (" + cures + " observed diseased stops)",
                new SkillRequirement(AccountSkill.MAGIC, 66, true),
                new SpellbookRequirement(SpellbookType.LUNAR), new QuestRequirement(Quest.LUNAR_DIPLOMACY)).evaluate(context));
        }
        long replants = plan.getStops().stream().filter(HerbRunStop::isReplant).count();
        if (compost != null && compost.isBottomless() && replants > 0)
        {
            // Filled buckets share an item ID for every compost type. Leprechaun storage varbits
            // do not establish the contents of an arbitrary carried/banked bucket.
            checks.add(RequirementResult.dataRequired(compost.toString(), "Manually check the selected compost type and at least "
                    + replants + " remaining uses. Bucket contents and charges are not verified."));
        }
        return new HerbRunCareResources(cure, runes.build(), checks);
    }

    public HerbRunCure getCure() { return cure; }
    public RuneCost getRunes() { return runes; }
    public List<RequirementResult> getChecks() { return checks; }
}
