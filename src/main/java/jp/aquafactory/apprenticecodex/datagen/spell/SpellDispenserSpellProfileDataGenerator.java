package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfile;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileDefinition;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileList;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SpellDispenserSpellProfileDataGenerator extends JsonCodecProvider<SpellDispenserSpellProfileList> {
    public SpellDispenserSpellProfileDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                SpellDispenserSpellProfileManager.DIRECTORY,
                PackType.SERVER_DATA,
                SpellDispenserSpellProfileList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                new SpellDispenserSpellProfileList(List.of(
                        profile("irons_spellbooks", "acupuncture", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "blood_needles", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "blood_slash", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "devour", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "ray_of_siphoning", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "wither_skull", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "sculk_tentacles", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "sonic_boom", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "black_hole", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "dragon_breath", SpellDispenserSpellProfile.MINIMUM_CONE),
                        profile("irons_spellbooks", "magic_arrow", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "magic_missile", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "shadow_slash", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "starfall", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "arrow_volley", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "chain_creeper", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "fang_strike", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "fang_ward", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "firecracker", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "gust", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "lob_creeper", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "shield", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "slow", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "spectral_hammer", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "blaze_storm", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "fire_arrow", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "fire_breath", SpellDispenserSpellProfile.MINIMUM_CONE),
                        profile("irons_spellbooks", "fireball", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "firebolt", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "flaming_strike", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "magma_bomb", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "scorch", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "blessing_of_life", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "cleanse", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "divine_smite", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "fortify", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "guiding_bolt", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "haste", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "healing_circle", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "sunbeam", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "wisp", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "cone_of_cold", SpellDispenserSpellProfile.MINIMUM_CONE),
                        profile("irons_spellbooks", "frostwave", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "ice_block", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "ice_spikes", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "icicle", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "ray_of_frost", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "snowball", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "ball_lightning", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "chain_lightning", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "electrocute", SpellDispenserSpellProfile.MINIMUM_CONE),
                        profile("irons_spellbooks", "lightning_bolt", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "lightning_lance", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "shockwave", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "acid_orb", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "blight", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "earthquake", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "firefly_swarm", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "poison_arrow", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "poison_breath", SpellDispenserSpellProfile.MINIMUM_CONE),
                        profile("irons_spellbooks", "poison_splash", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "root", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "stomp", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "touch_dig", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "arcane_blast", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "arcane_beam", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "quick_arms", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "feather_rush", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "slash_blade", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "precision_jack", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "auto_turret", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "search_beacon", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "thermal_process", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "mage_light", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "force_field", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "illuminate_stellar", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "unite_luna", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "sky_edge", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "breaching_enemy", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "bullet_stream", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "fly_swatter", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "shock", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "compound_phial", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "tiny_lumberjack", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "graced_rain", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "world_flatter", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "earth_forge", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "grind_runner", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "harvest_moon", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "moon_light", SpellDispenserSpellProfile.DEFAULT)
                ))
        );
    }

    private static SpellDispenserSpellProfileDefinition profile(
            String namespace,
            String path,
            SpellDispenserSpellProfile profile
    ) {
        return new SpellDispenserSpellProfileDefinition(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                profile
        );
    }
}
