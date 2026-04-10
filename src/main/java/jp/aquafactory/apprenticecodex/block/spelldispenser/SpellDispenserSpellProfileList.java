package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record SpellDispenserSpellProfileList(List<SpellDispenserSpellProfileDefinition> values) {
    public static final Codec<SpellDispenserSpellProfileList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    SpellDispenserSpellProfileDefinition.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(SpellDispenserSpellProfileList::values)
            ).apply(instance, SpellDispenserSpellProfileList::new)
    );
}
