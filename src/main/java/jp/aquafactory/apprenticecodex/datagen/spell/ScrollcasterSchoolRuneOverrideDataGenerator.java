package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneOverrideManager;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneOverrides;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.Map;

public final class ScrollcasterSchoolRuneOverrideDataGenerator extends JsonCodecProvider<ScrollcasterSchoolRuneOverrides> {
    public ScrollcasterSchoolRuneOverrideDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                ScrollcasterSchoolRuneOverrideManager.DIRECTORY,
                ScrollcasterSchoolRuneOverrides.CODEC,
                Map.of()
        );
    }
}
