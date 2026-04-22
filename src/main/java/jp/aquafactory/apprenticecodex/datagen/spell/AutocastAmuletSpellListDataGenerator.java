package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellList;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellListManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AutocastAmuletSpellListDataGenerator extends JsonCodecProvider<AutocastAmuletSpellList> {
    public AutocastAmuletSpellListDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                AutocastAmuletSpellListManager.DIRECTORY,
                PackType.SERVER_DATA,
                AutocastAmuletSpellList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
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
        );
    }
}
