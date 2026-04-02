package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightEntityList;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightManager;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightVariant;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SenseEvilHighlightDataGenerator extends JsonCodecProvider<SenseEvilHighlightEntityList> {
    public SenseEvilHighlightDataGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                           ExistingFileHelper existingFileHelper) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                SenseEvilHighlightManager.DIRECTORY,
                PackType.SERVER_DATA,
                SenseEvilHighlightEntityList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, SenseEvilHighlightVariant.STRONG.getDataFileName()),
                new SenseEvilHighlightEntityList(List.of(
                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "necromancer")
                ))
        );
    }
}
