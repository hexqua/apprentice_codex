package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneOverrideManager;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneOverrides;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.concurrent.CompletableFuture;

public final class ScrollcasterSchoolRuneOverrideDataGenerator extends JsonCodecProvider<ScrollcasterSchoolRuneOverrides> {
    public ScrollcasterSchoolRuneOverrideDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                ScrollcasterSchoolRuneOverrideManager.DIRECTORY,
                PackType.SERVER_DATA,
                ScrollcasterSchoolRuneOverrides.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
    }
}
