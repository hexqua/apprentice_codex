package jp.aquafactory.apprenticecodex.item.elementalbow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ElementalBowModeList(List<ElementalBowModeDefinition> values) {
    public static final Codec<ElementalBowModeList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ElementalBowModeDefinition.CODEC.listOf()
                    .fieldOf("values")
                    .forGetter(ElementalBowModeList::values)
    ).apply(instance, ElementalBowModeList::new));

    public ElementalBowModeList {
        values = List.copyOf(values);
    }
}
