package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfile;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileDefinition;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileList;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.Map;

public final class SpellDispenserSpellProfileDataGenerator extends JsonCodecProvider<SpellDispenserSpellProfileList> {
    public SpellDispenserSpellProfileDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SpellDispenserSpellProfileManager.DIRECTORY,
                SpellDispenserSpellProfileList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                        new SpellDispenserSpellProfileList(List.of(
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "heal"),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spectral_hammer"),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mage_light"),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "compound_phial"),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm"),
                                        SpellDispenserSpellProfile.DEFAULT
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath"),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "dragon_breath"),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "cone_of_cold"),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "poison_breath"),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                ),
                                new SpellDispenserSpellProfileDefinition(
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "electrocute"),
                                        SpellDispenserSpellProfile.MINIMUM_CONE
                                )
                        ))
                )
        );
    }
}
