package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinitySelectionPolicy;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinitySelectionPolicyManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.concurrent.CompletableFuture;

public final class SchoolAffinitySelectionPolicyDataGenerator extends JsonCodecProvider<SchoolAffinitySelectionPolicy> {
    public SchoolAffinitySelectionPolicyDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                SchoolAffinitySelectionPolicyManager.DIRECTORY,
                PackType.SERVER_DATA,
                SchoolAffinitySelectionPolicy.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
    }
}
