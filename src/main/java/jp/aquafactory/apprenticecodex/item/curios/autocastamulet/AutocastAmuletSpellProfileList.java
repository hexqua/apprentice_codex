package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record AutocastAmuletSpellProfileList(List<AutocastAmuletSpellProfileDefinition> values) {
    public static final Codec<AutocastAmuletSpellProfileList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    AutocastAmuletSpellProfileDefinition.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(AutocastAmuletSpellProfileList::values)
            ).apply(instance, AutocastAmuletSpellProfileList::new)
    );
}
