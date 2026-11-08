package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import net.minecraft.resources.ResourceLocation;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.MALUM_MOD_ID;

public final class EnchantmentApplicationGameTestSupport {
    private EnchantmentApplicationGameTestSupport() {}

    public static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "spirit_plunder");

    public static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "replenishing");
}
