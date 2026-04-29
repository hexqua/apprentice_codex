package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfile;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileDefinition;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileList;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellProfileManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChargedTwinBladeStaffSpellProfileDataGenerator extends JsonCodecProvider<ChargedTwinBladeStaffSpellProfileList> {
    public ChargedTwinBladeStaffSpellProfileDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                ChargedTwinBladeStaffSpellProfileManager.DIRECTORY,
                PackType.SERVER_DATA,
                ChargedTwinBladeStaffSpellProfileList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                new ChargedTwinBladeStaffSpellProfileList(List.of(
                        profile("irons_spellbooks", "blood_step", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "heartstop", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "raise_dead", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "planar_sight", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "counterspell", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "echoing_strikes", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "evasion", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "summon_swords", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "invisibility", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "summon_horse", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "summon_vex", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "burning_dash", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "heat_surge", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "angel_wings", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "cleanse", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "fortify", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "greater_heal", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "haste", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "heal", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "ice_tomb", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "summon_polar_bear", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "ascension", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "charge", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "shockwave", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile("irons_spellbooks", "volt_strike", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "gluttony", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "oakskin", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile("irons_spellbooks", "spider_aspect", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile(ApprenticeCodex.MODID, "archer_multiple", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile(ApprenticeCodex.MODID, "higanbana", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile(ApprenticeCodex.MODID, "auto_magnet", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile(ApprenticeCodex.MODID, "companion_trunk", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile(ApprenticeCodex.MODID, "sense_evil", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile(ApprenticeCodex.MODID, "commence_fire", ChargedTwinBladeStaffSpellProfile.IMPACT_PROXY_OWNER_MAGIC_INITIAL_RECAST),
                        profile(ApprenticeCodex.MODID, "deep_sensor", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF),
                        profile(ApprenticeCodex.MODID, "spectral_wing", ChargedTwinBladeStaffSpellProfile.PLAYER_SELF)
                ))
        );
    }

    private static ChargedTwinBladeStaffSpellProfileDefinition profile(
            String namespace,
            String path,
            ChargedTwinBladeStaffSpellProfile profile
    ) {
        return new ChargedTwinBladeStaffSpellProfileDefinition(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                profile
        );
    }
}
