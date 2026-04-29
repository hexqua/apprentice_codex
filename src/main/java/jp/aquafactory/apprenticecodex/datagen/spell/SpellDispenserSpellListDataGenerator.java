package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellList;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellListManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.Map;

public final class SpellDispenserSpellListDataGenerator extends JsonCodecProvider<SpellDispenserSpellList> {
    public SpellDispenserSpellListDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SpellDispenserSpellListManager.DIRECTORY,
                SpellDispenserSpellList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "denylist"),
                        new SpellDispenserSpellList(List.of(
                                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "assist_wings"),
                                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "auto_magnet"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ray_of_siphoning")
                        ))
                )
        );
    }
}
