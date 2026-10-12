package com.farmingnavigator.routing.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.ItemID;

public enum ChargedTeleportItemDefinition
{
    BURNING_AMULET(
            "Burning amulet",
            finiteVariants(
                    "Burning amulet",
                    ItemID.BURNING_AMULET_1,
                    ItemID.BURNING_AMULET_2,
                    ItemID.BURNING_AMULET_3,
                    ItemID.BURNING_AMULET_4,
                    ItemID.BURNING_AMULET_5
            )
    ),

    COMBAT_BRACELET(
            "Combat bracelet",
            finiteVariants(
                    "Combat bracelet",
                    ItemID.JEWL_BRACELET_OF_COMBAT_1,
                    ItemID.JEWL_BRACELET_OF_COMBAT_2,
                    ItemID.JEWL_BRACELET_OF_COMBAT_3,
                    ItemID.JEWL_BRACELET_OF_COMBAT_4,
                    ItemID.JEWL_BRACELET_OF_COMBAT_5,
                    ItemID.JEWL_BRACELET_OF_COMBAT_6
            )
    ),

    DIGSITE_PENDANT(
            "Digsite pendant",
            finiteVariants(
                    "Digsite pendant",
                    ItemID.NECKLACE_OF_DIGSITE_1,
                    ItemID.NECKLACE_OF_DIGSITE_2,
                    ItemID.NECKLACE_OF_DIGSITE_3,
                    ItemID.NECKLACE_OF_DIGSITE_4,
                    ItemID.NECKLACE_OF_DIGSITE_5
            )
    ),

    GAMES_NECKLACE(
            "Games necklace",
            finiteVariants(
                    "Games necklace",
                    ItemID.NECKLACE_OF_MINIGAMES_1,
                    ItemID.NECKLACE_OF_MINIGAMES_2,
                    ItemID.NECKLACE_OF_MINIGAMES_3,
                    ItemID.NECKLACE_OF_MINIGAMES_4,
                    ItemID.NECKLACE_OF_MINIGAMES_5,
                    ItemID.NECKLACE_OF_MINIGAMES_6,
                    ItemID.NECKLACE_OF_MINIGAMES_7,
                    ItemID.NECKLACE_OF_MINIGAMES_8
            )
    ),

    AMULET_OF_GLORY(
            "Amulet of glory",
            combine(
                    finiteVariants(
                            "Amulet of glory",
                            ItemID.AMULET_OF_GLORY_1,
                            ItemID.AMULET_OF_GLORY_2,
                            ItemID.AMULET_OF_GLORY_3,
                            ItemID.AMULET_OF_GLORY_4,
                            ItemID.AMULET_OF_GLORY_5,
                            ItemID.AMULET_OF_GLORY_6
                    ),
                    finiteVariants(
                            "Amulet of glory (t)",
                            ItemID.TRAIL_AMULET_OF_GLORY_1,
                            ItemID.TRAIL_AMULET_OF_GLORY_2,
                            ItemID.TRAIL_AMULET_OF_GLORY_3,
                            ItemID.TRAIL_AMULET_OF_GLORY_4,
                            ItemID.TRAIL_AMULET_OF_GLORY_5,
                            ItemID.TRAIL_AMULET_OF_GLORY_6
                    ),
                    Collections.singletonList(
                            unlimited(
                                    ItemID.AMULET_OF_GLORY_INF,
                                    "Amulet of eternal glory"
                            )
                    )
            )
    ),

    NECKLACE_OF_PASSAGE(
            "Necklace of passage",
            finiteVariants(
                    "Necklace of passage",
                    ItemID.NECKLACE_OF_PASSAGE_1,
                    ItemID.NECKLACE_OF_PASSAGE_2,
                    ItemID.NECKLACE_OF_PASSAGE_3,
                    ItemID.NECKLACE_OF_PASSAGE_4,
                    ItemID.NECKLACE_OF_PASSAGE_5
            )
    ),

    RING_OF_DUELING(
            "Ring of dueling",
            finiteVariants(
                    "Ring of dueling",
                    ItemID.RING_OF_DUELING_1,
                    ItemID.RING_OF_DUELING_2,
                    ItemID.RING_OF_DUELING_3,
                    ItemID.RING_OF_DUELING_4,
                    ItemID.RING_OF_DUELING_5,
                    ItemID.RING_OF_DUELING_6,
                    ItemID.RING_OF_DUELING_7,
                    ItemID.RING_OF_DUELING_8
            )
    ),

    SLAYER_RING(
            "Slayer ring",
            combine(
                    finiteVariants(
                            "Slayer ring",
                            ItemID.SLAYER_RING_1,
                            ItemID.SLAYER_RING_2,
                            ItemID.SLAYER_RING_3,
                            ItemID.SLAYER_RING_4,
                            ItemID.SLAYER_RING_5,
                            ItemID.SLAYER_RING_6,
                            ItemID.SLAYER_RING_7,
                            ItemID.SLAYER_RING_8
                    ),
                    Collections.singletonList(
                            unlimited(
                                    ItemID.SLAYER_RING_ETERNAL,
                                    "Eternal slayer ring"
                            )
                    )
            )
    ),

    RING_OF_WEALTH(
            "Ring of wealth",
            combine(
                    finiteVariants(
                            "Ring of wealth",
                            ItemID.RING_OF_WEALTH_1,
                            ItemID.RING_OF_WEALTH_2,
                            ItemID.RING_OF_WEALTH_3,
                            ItemID.RING_OF_WEALTH_4,
                            ItemID.RING_OF_WEALTH_5
                    ),
                    finiteVariants(
                            "Ring of wealth (i)",
                            ItemID.RING_OF_WEALTH_I1,
                            ItemID.RING_OF_WEALTH_I2,
                            ItemID.RING_OF_WEALTH_I3,
                            ItemID.RING_OF_WEALTH_I4,
                            ItemID.RING_OF_WEALTH_I5
                    )
            )
    ),

    SKILLS_NECKLACE(
            "Skills necklace",
            finiteVariants(
                    "Skills necklace",
                    ItemID.JEWL_NECKLACE_OF_SKILLS_1,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_4,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_5,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_6
            )
    );

    private final String displayName;
    private final List<Variant> variants;

    ChargedTeleportItemDefinition(
            String displayName,
            List<Variant> variants
    )
    {
        this.displayName = displayName;

        this.variants = Collections.unmodifiableList(
                new ArrayList<>(
                        variants
                )
        );
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public List<Variant> getVariants()
    {
        return variants;
    }

    public Variant findVariant(
            int itemId
    )
    {
        for (
                Variant variant
                : variants
        )
        {
            if (
                    variant.getItemId()
                            == itemId
            )
            {
                return variant;
            }
        }

        return null;
    }

    public ChargedTeleportItemRequirement
    createRequirement()
    {
        return createRequirement(
                1L
        );
    }

    public ChargedTeleportItemRequirement
    createRequirement(
            long requiredCharges
    )
    {
        return new ChargedTeleportItemRequirement(
                this,
                requiredCharges
        );
    }

    private static List<Variant>
    finiteVariants(
            String baseDisplayName,
            int... itemIds
    )
    {
        List<Variant> result =
                new ArrayList<>();

        for (
                int index = 0;
                index < itemIds.length;
                index++
        )
        {
            int charges =
                    index + 1;

            result.add(
                    new Variant(
                            itemIds[index],
                            baseDisplayName
                                    + "("
                                    + charges
                                    + ")",
                            charges,
                            false
                    )
            );
        }

        return result;
    }

    @SafeVarargs
    private static List<Variant>
    combine(
            List<Variant>... groups
    )
    {
        List<Variant> result =
                new ArrayList<>();

        if (groups != null)
        {
            Arrays.stream(
                            groups
                    )
                    .filter(
                            group ->
                                    group != null
                    )
                    .forEach(
                            result::addAll
                    );
        }

        return result;
    }

    private static Variant unlimited(
            int itemId,
            String displayName
    )
    {
        return new Variant(
                itemId,
                displayName,
                0,
                true
        );
    }

    public static final class Variant
    {
        private final int itemId;
        private final String displayName;
        private final int charges;
        private final boolean unlimited;

        private Variant(
                int itemId,
                String displayName,
                int charges,
                boolean unlimited
        )
        {
            if (itemId <= 0)
            {
                throw new IllegalArgumentException(
                        "Charged teleport item ID must be positive."
                );
            }

            if (
                    displayName == null
                            || displayName
                            .trim()
                            .isEmpty()
            )
            {
                throw new IllegalArgumentException(
                        "Charged teleport item display name cannot be blank."
                );
            }

            if (
                    !unlimited
                            && charges <= 0
            )
            {
                throw new IllegalArgumentException(
                        "Finite charged teleport variants must have at least one charge."
                );
            }

            this.itemId =
                    itemId;

            this.displayName =
                    displayName;

            this.charges =
                    charges;

            this.unlimited =
                    unlimited;
        }

        public int getItemId()
        {
            return itemId;
        }

        public String getDisplayName()
        {
            return displayName;
        }

        public int getCharges()
        {
            return charges;
        }

        public boolean isUnlimited()
        {
            return unlimited;
        }
    }
}