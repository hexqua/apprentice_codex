package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record SpellDispenserSpellProfileDefinition(
        ResourceLocation spell,
        SpellDispenserSpellProfile profile
) {
    public static final Codec<SpellDispenserSpellProfileDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("spell").forGetter(SpellDispenserSpellProfileDefinition::spell),
                    SpellDispenserSpellProfile.CODEC.optionalFieldOf("profile", SpellDispenserSpellProfile.DEFAULT)
                            .forGetter(SpellDispenserSpellProfileDefinition::profile)
            ).apply(instance, SpellDispenserSpellProfileDefinition::new)
    );
}
