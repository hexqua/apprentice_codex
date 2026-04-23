package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record AutocastAmuletSpellList(List<ResourceLocation> values) {
    public static final Codec<AutocastAmuletSpellList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(AutocastAmuletSpellList::values)
            ).apply(instance, AutocastAmuletSpellList::new)
    );
}
