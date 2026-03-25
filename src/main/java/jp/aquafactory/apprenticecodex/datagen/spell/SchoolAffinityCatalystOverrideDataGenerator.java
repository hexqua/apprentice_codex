package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityCatalystOverrideManager;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityCatalystOverrides;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.Map;

public final class SchoolAffinityCatalystOverrideDataGenerator extends JsonCodecProvider<SchoolAffinityCatalystOverrides> {
    public SchoolAffinityCatalystOverrideDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SchoolAffinityCatalystOverrideManager.DIRECTORY,
                SchoolAffinityCatalystOverrides.CODEC,
                Map.of()
        );
    }
}
