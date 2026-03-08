package jp.aquafactory.apprenticecodex.spell.senseevil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SenseEvilHighlightEntityList(List<ResourceLocation> values) {
    public static final Codec<SenseEvilHighlightEntityList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.listOf()
                            .optionalFieldOf("values", List.of())
                            .forGetter(SenseEvilHighlightEntityList::values)
            ).apply(instance, SenseEvilHighlightEntityList::new)
    );

    public SenseEvilHighlightEntityList {
        values = List.copyOf(values);
    }
}
