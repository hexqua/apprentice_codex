package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellList;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellListManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.Map;

public final class AutocastAmuletSpellListDataGenerator extends JsonCodecProvider<AutocastAmuletSpellList> {
    public AutocastAmuletSpellListDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                AutocastAmuletSpellListManager.DIRECTORY,
                AutocastAmuletSpellList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "allowlist"),
                        new AutocastAmuletSpellList(List.of(
                                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sense_evil"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "heartstop"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "abyssal_shroud"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "planar_sight"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "echoing_strikes"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "evasion"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "invisibility"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "angel_wing"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "cleanse"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fortify"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "greater_heal"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "haste"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "heal"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ice_tomb"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "charge"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "thunderstorm"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "gluttony"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spider_aspect")
                        ))
                )
        );
    }
}
