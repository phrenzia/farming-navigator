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
    // https://oldschool.runescape.wiki/w/Fertile_Soil : level 83, three astral, two nature, fifteen earth.
    public static final RuneCost FERTILE_SOIL_COST = RuneCost.builder().add(RuneKind.ASTRAL, 3)
            .add(RuneKind.NATURE, 2).add(RuneKind.EARTH, 15).build();
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
        if (compost != null && compost.isSpell() && replants > 0)
        {
            FERTILE_SOIL_COST.getQuantities().forEach((kind, quantity) -> runes.add(kind, quantity * (int) replants));
            checks.add(new AllOfRequirement("Fertile Soil access (" + replants + " potential replants)",
                    new SkillRequirement(AccountSkill.MAGIC, 83, true), new SpellbookRequirement(SpellbookType.LUNAR),
                    new QuestRequirement(Quest.LUNAR_DIPLOMACY)).evaluate(context));
            if (compost == HerbRunCompost.FERTILE_SOIL_ULTRA)
            {
                checks.add(context != null && context.getConfig() != null && context.getConfig().herbAshTomeRead()
                        ? RequirementResult.satisfied("Ash-covered tome", "Manually declared as read; this unlock is not automatically verified.")
                        : RequirementResult.configurationRequired("Ash-covered tome",
                                "Ultracompost requires reading the ash-covered tome. Confirm this unlock in plugin settings; it is not automatically verified."));
            }
            else
            {
                checks.add(RequirementResult.satisfied("Fertile Soil supercompost supplies",
                        "No volcanic ash is reserved in this mode. An upgraded spell can consume carried ash for ultracompost."));
            }
        }
        return new HerbRunCareResources(cure, runes.build(), checks);
    }

    public HerbRunCure getCure() { return cure; }
    public RuneCost getRunes() { return runes; }
    public List<RequirementResult> getChecks() { return checks; }
}
