package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record AutocastAmuletSpellProfileDefinition(
        ResourceLocation spell,
        AutocastAmuletSpellProfile profile
) {
    public static final Codec<AutocastAmuletSpellProfileDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("spell").forGetter(AutocastAmuletSpellProfileDefinition::spell),
                    AutocastAmuletSpellProfile.CODEC.fieldOf("profile")
                            .forGetter(AutocastAmuletSpellProfileDefinition::profile)
            ).apply(instance, AutocastAmuletSpellProfileDefinition::new)
    );
}
