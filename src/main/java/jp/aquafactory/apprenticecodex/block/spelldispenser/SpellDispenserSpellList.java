package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SpellDispenserSpellList(List<ResourceLocation> values) {
    public static final Codec<SpellDispenserSpellList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(SpellDispenserSpellList::values)
            ).apply(instance, SpellDispenserSpellList::new)
    );
}
