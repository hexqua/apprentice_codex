package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class MalumTestSupport {
    private MalumTestSupport() {}

    static final String LODESTONE_MOD_ID = "lodestone";

    public static final String MALUM_MOD_ID = "malum";

    static final ResourceLocation LODESTONE_MAGIC_PROFICIENCY =
            ResourceLocation.fromNamespaceAndPath(LODESTONE_MOD_ID, "magic_proficiency");

    public static final TagKey<Item> MALUM_SOUL_SHATTER_CAPABLE_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soul_shatter_capable_weapon")
    );

    public static final TagKey<Item> MALUM_SOULWOVEN_POUCH_EFFICIENT = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "soulwoven_pouch_efficient")
    );

    public static final TagKey<Item> MALUM_MAGIC_CAPABLE_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "magic_capable_weapon")
    );

    public static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");

    public static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "replenishing");

    public static final ResourceLocation MALUM_HAUNTED =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "haunted");

    public static final ResourceLocation MALUM_ANIMATED =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "animated");
}
