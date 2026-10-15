package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.capability.SpellbookType;
import java.util.Collections;
import java.util.List;
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
        if (cure == null) { cure = HerbRunCure.PLANT_CURE; }
        int cures = (int) plan.getStops().stream().filter(stop -> !stop.isReplant()).count();
        if (cure != HerbRunCure.CURE_PLANT || cures == 0)
        {
            return new HerbRunCareResources(cure, RuneCost.builder().build(), Collections.emptyList());
        }
        RuneCost.Builder runes = RuneCost.builder();
        CURE_PLANT_COST.getQuantities().forEach((kind, quantity) -> runes.add(kind, quantity * cures));
        RequirementResult eligibility = new AllOfRequirement("Cure Plant access (" + cures + " observed diseased stops)",
                new SkillRequirement(AccountSkill.MAGIC, 66, true),
                new SpellbookRequirement(SpellbookType.LUNAR), new QuestRequirement(Quest.LUNAR_DIPLOMACY)).evaluate(context);
        return new HerbRunCareResources(cure, runes.build(), Collections.singletonList(eligibility));
    }

    public HerbRunCure getCure() { return cure; }
    public RuneCost getRunes() { return runes; }
    public List<RequirementResult> getChecks() { return checks; }
}
