package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.SoulcollectorRobeItem;
import net.minecraft.resources.ResourceLocation;

/**
 * Soulcollector は Enchantress と同じボーン構成のため、揺れ制御を共通モデルから継承する。
 * 1.21.1 側では GeckoLib のモデル API 差分を確認して移植すること。
 */
public final class SoulcollectorRobeModel extends EnchantressRobeModel<SoulcollectorRobeItem> {
    public SoulcollectorRobeModel() {
        super(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/soulcollector_robe.geo.json"),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/soulcollector_robe.png"),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/soulcollector_robe.animation.json")
        );
    }
}
