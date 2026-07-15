package jp.aquafactory.apprenticecodex.item.spellgun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record SpellGunSpellList(List<ResourceLocation> values) {
    public static final Codec<SpellGunSpellList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(SpellGunSpellList::values)
            ).apply(instance, SpellGunSpellList::new)
    );
}
