package jp.aquafactory.apprenticecodex.item.elementalbow;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ElementalBowClientConfigState {
    private static double schoolRuneManaCostMultiplier = 2.0D;

    public static double schoolRuneManaCostMultiplier() { return schoolRuneManaCostMultiplier; }

    public static void setSchoolRuneManaCostMultiplier(double value) {
        schoolRuneManaCostMultiplier = Double.isFinite(value) ? Math.clamp(value, 1.0D, 10.0D) : 2.0D;
    }
    private static final List<ResourceLocation> DEFAULT_MAGIC_ARROW_CATALYST_ITEM_IDS =
            List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "arrow"));
    private static List<ResourceLocation> magicArrowCatalystItemIds = DEFAULT_MAGIC_ARROW_CATALYST_ITEM_IDS;

    private ElementalBowClientConfigState() {
    }

    public static void setMagicArrowCatalystItemIds(List<ResourceLocation> magicArrowCatalystItemIds) {
        ElementalBowClientConfigState.magicArrowCatalystItemIds = List.copyOf(magicArrowCatalystItemIds);
    }

    public static void reset() {
        schoolRuneManaCostMultiplier = 2.0D;
        magicArrowCatalystItemIds = DEFAULT_MAGIC_ARROW_CATALYST_ITEM_IDS;
    }

    public static List<ResourceLocation> magicArrowCatalystItemIds() {
        return magicArrowCatalystItemIds;
    }
}
