package jp.aquafactory.apprenticecodex.item.elementalbow;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ElementalBowClientConfigState {
    private static final List<ResourceLocation> DEFAULT_MAGIC_ARROW_CATALYST_ITEM_IDS =
            List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "arrow"));
    private static List<ResourceLocation> magicArrowCatalystItemIds = DEFAULT_MAGIC_ARROW_CATALYST_ITEM_IDS;

    private ElementalBowClientConfigState() {
    }

    public static void setMagicArrowCatalystItemIds(List<ResourceLocation> magicArrowCatalystItemIds) {
        ElementalBowClientConfigState.magicArrowCatalystItemIds = List.copyOf(magicArrowCatalystItemIds);
    }

    public static void reset() {
        magicArrowCatalystItemIds = DEFAULT_MAGIC_ARROW_CATALYST_ITEM_IDS;
    }

    public static List<ResourceLocation> magicArrowCatalystItemIds() {
        return magicArrowCatalystItemIds;
    }
}
