package jp.aquafactory.apprenticecodex.damage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class ApprenticeDamageTypeTags {
    public static final TagKey<DamageType> TRIGGERS_IRONS_JEWELRY_PROJECTILE_HIT = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    "triggers_irons_jewelry_projectile_hit"
            )
    );

    private ApprenticeDamageTypeTags() {
    }
}
