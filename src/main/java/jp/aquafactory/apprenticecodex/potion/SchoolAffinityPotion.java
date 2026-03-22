package jp.aquafactory.apprenticecodex.potion;

import jp.aquafactory.apprenticecodex.effect.SchoolAffinityEffect;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
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

    public SchoolAffinityPotion(int slotIndex, SchoolAffinityPotionVariant variant, SchoolAffinityEffect effect) {
        super(
                variant.registryNamePrefix(),
                new MobEffectInstance(effect, variant.durationTicks(), variant.amplifier())
        );
        this.slotIndex = slotIndex;
        this.variant = variant;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public SchoolAffinityPotionVariant getVariant() {
        return variant;
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
