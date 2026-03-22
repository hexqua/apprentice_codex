package jp.aquafactory.apprenticecodex.potion;

import jp.aquafactory.apprenticecodex.effect.SchoolAffinityEffect;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.Potion;

public class SchoolAffinityPotion extends Potion {
    private final int slotIndex;
    private final SchoolAffinityPotionVariant variant;
    private final SchoolAffinityEffect effect;

    public SchoolAffinityPotion(int slotIndex, SchoolAffinityPotionVariant variant, SchoolAffinityEffect effect) {
        super(variant.registryNamePrefix());
        this.slotIndex = slotIndex;
        this.variant = variant;
        this.effect = effect;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public SchoolAffinityPotionVariant getVariant() {
        return variant;
    }

    @Override
    public java.util.List<MobEffectInstance> getEffects() {
        // 動的エフェクトは定義生成時点では未登録のため、参照 Holder は利用時に引き直す.
        return java.util.List.of(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                variant.durationTicks(),
                variant.amplifier()
        ));
    }

    public Component getItemDisplayName(Item item) {
        var schoolType = SchoolAffinityRegistry.getAssignedSchool(slotIndex).orElse(null);
        var affinityName = schoolType != null
                ? SchoolAffinityRegistry.createAffinityName(schoolType)
                : Component.translatable("effect.apprenticecodex.school_affinity");
        var translationKey = "item.apprenticecodex.school_affinity.potion";

        if (item instanceof SplashPotionItem) {
            translationKey = "item.apprenticecodex.school_affinity.splash_potion";
        } else if (item instanceof LingeringPotionItem) {
            translationKey = "item.apprenticecodex.school_affinity.lingering_potion";
        } else if (item instanceof ArrowItem) {
            translationKey = "item.apprenticecodex.school_affinity.tipped_arrow";
        }

        return Component.translatable(translationKey, affinityName);
    }
}
