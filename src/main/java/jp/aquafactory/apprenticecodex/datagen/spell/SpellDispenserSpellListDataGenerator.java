package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellList;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellListManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SpellDispenserSpellListDataGenerator extends JsonCodecProvider<SpellDispenserSpellList> {
    public SpellDispenserSpellListDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                SpellDispenserSpellListManager.DIRECTORY,
                PackType.SERVER_DATA,
                SpellDispenserSpellList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "denylist"),
                new SpellDispenserSpellList(List.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "assist_wings"),
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "auto_magnet"),
                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ray_of_siphoning")
                ))
        );
    }
}
