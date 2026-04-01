package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightEntityList;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightManager;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightVariant;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.Map;

public class SenseEvilHighlightDataGenerator extends JsonCodecProvider<SenseEvilHighlightEntityList> {
    public SenseEvilHighlightDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SenseEvilHighlightManager.DIRECTORY,
                SenseEvilHighlightEntityList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, SenseEvilHighlightVariant.STRONG.getDataFileName()),
                        new SenseEvilHighlightEntityList(java.util.List.of(
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "necromancer")
                        ))
                )
        );
    }
}
