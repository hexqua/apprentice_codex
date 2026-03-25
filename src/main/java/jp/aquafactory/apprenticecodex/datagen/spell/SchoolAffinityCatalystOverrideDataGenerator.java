package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityCatalystOverrideManager;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityCatalystOverrides;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.concurrent.CompletableFuture;

public final class SchoolAffinityCatalystOverrideDataGenerator extends JsonCodecProvider<SchoolAffinityCatalystOverrides> {
    public SchoolAffinityCatalystOverrideDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                SchoolAffinityCatalystOverrideManager.DIRECTORY,
                PackType.SERVER_DATA,
                SchoolAffinityCatalystOverrides.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
    }
}
