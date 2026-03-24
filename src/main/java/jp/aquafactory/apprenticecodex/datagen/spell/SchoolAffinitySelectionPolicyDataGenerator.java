package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinitySelectionPolicy;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinitySelectionPolicyManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.Map;

public final class SchoolAffinitySelectionPolicyDataGenerator extends JsonCodecProvider<SchoolAffinitySelectionPolicy> {
    public SchoolAffinitySelectionPolicyDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SchoolAffinitySelectionPolicyManager.DIRECTORY,
                SchoolAffinitySelectionPolicy.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "default"),
                        SchoolAffinitySelectionPolicy.EMPTY
                )
        );
    }
}
