package jp.aquafactory.apprenticecodex.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public final class LootTableGenerator extends LootTableProvider {
    public LootTableGenerator(PackOutput output) {
        super(
                output,
                Set.of(),
                List.of(new SubProviderEntry(BlockLootTableGenerator::new, LootContextParamSets.BLOCK))
        );
    }
}
