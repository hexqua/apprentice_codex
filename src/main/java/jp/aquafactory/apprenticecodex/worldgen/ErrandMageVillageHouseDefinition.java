package jp.aquafactory.apprenticecodex.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ErrandMageVillageHouseDefinition(
        ResourceLocation pool,
        ResourceLocation structure,
        ResourceLocation processor,
        int weight
) {
    public static final Codec<ErrandMageVillageHouseDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("pool").forGetter(ErrandMageVillageHouseDefinition::pool),
                    ResourceLocation.CODEC.fieldOf("structure").forGetter(ErrandMageVillageHouseDefinition::structure),
                    ResourceLocation.CODEC.fieldOf("processor").forGetter(ErrandMageVillageHouseDefinition::processor),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("weight").forGetter(ErrandMageVillageHouseDefinition::weight)
            ).apply(instance, ErrandMageVillageHouseDefinition::new)
    );
}
