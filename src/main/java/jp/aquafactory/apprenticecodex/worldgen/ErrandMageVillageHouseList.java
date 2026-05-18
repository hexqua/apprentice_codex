package jp.aquafactory.apprenticecodex.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ErrandMageVillageHouseList(List<ErrandMageVillageHouseDefinition> values) {
    public static final Codec<ErrandMageVillageHouseList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ErrandMageVillageHouseDefinition.CODEC.listOf()
                            .fieldOf("values")
                            .forGetter(ErrandMageVillageHouseList::values)
            ).apply(instance, ErrandMageVillageHouseList::new)
    );

    public ErrandMageVillageHouseList {
        values = List.copyOf(values);
    }
}
