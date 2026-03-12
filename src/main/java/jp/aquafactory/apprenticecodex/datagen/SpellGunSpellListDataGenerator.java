package jp.aquafactory.apprenticecodex.datagen;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SpellGunSpellList;
import jp.aquafactory.apprenticecodex.item.SpellGunSpellListManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.Map;

public final class SpellGunSpellListDataGenerator extends JsonCodecProvider<SpellGunSpellList> {
    public SpellGunSpellListDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SpellGunSpellListManager.DIRECTORY,
                SpellGunSpellList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "denylist"),
                        new SpellGunSpellList(List.of())
                )
        );
    }
}
