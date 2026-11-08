package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.MALUM_MOD_ID;

public final class MalumTestSupport {
    private MalumTestSupport() {}

    public static final String LODESTONE_MOD_ID = "lodestone";

    public static final ResourceLocation LODESTONE_MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath(LODESTONE_MOD_ID, "magic_proficiency");

    public static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_hunter_weapon")
    );

    public static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("traveloptics", "can_cast_reversal")
    );

    public static final ResourceLocation MALUM_SPIRIT_PLUNDER = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");

    static final ResourceLocation MALUM_REPLENISHING = ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "replenishing");
}
