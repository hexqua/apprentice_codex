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
                        profile("irons_spellbooks", "wither_skull", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "sculk_tentacles", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "sonic_boom", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "black_hole", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "dragon_breath", SpellDispenserSpellProfile.CONE_BACKWARD),
                        profile("irons_spellbooks", "magic_arrow", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "magic_missile", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "shadow_slash", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "starfall", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "arrow_volley", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "chain_creeper", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "fang_strike", SpellDispenserSpellProfile.OWNER_OPTIONAL_BACKWARD),
                        profile("irons_spellbooks", "fang_ward", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "firecracker", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "gust", SpellDispenserSpellProfile.OWNER_OPTIONAL_BACKWARD),
                        profile("irons_spellbooks", "lob_creeper", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "shield", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "slow", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "spectral_hammer", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "blaze_storm", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "fire_arrow", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "fire_breath", SpellDispenserSpellProfile.CONE_BACKWARD),
                        profile("irons_spellbooks", "fireball", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "firebolt", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "flaming_strike", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "magma_bomb", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "scorch", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "blessing_of_life", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "cleanse", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "divine_smite", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "fortify", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "guiding_bolt", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "haste", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "healing_circle", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "sunbeam", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "wisp", SpellDispenserSpellProfile.OWNER_OPTIONAL_BACKWARD),
                        profile("irons_spellbooks", "cone_of_cold", SpellDispenserSpellProfile.CONE_BACKWARD),
                        profile("irons_spellbooks", "frostwave", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "ice_block", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "ice_spikes", SpellDispenserSpellProfile.OWNER_OPTIONAL_BACKWARD),
                        profile("irons_spellbooks", "icicle", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "ray_of_frost", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "snowball", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "ball_lightning", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "chain_lightning", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "electrocute", SpellDispenserSpellProfile.MINIMUM_CONE),
                        profile("irons_spellbooks", "lightning_bolt", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "lightning_lance", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "shockwave", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "acid_orb", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "blight", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "earthquake", SpellDispenserSpellProfile.DEFAULT),
                        profile("irons_spellbooks", "firefly_swarm", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "poison_arrow", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "poison_breath", SpellDispenserSpellProfile.CONE_BACKWARD),
                        profile("irons_spellbooks", "poison_splash", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "root", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "stomp", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile("irons_spellbooks", "touch_dig", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "arcane_blast", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "arcane_beam", SpellDispenserSpellProfile.OWNER_OPTIONAL_BACKWARD),
                        profile(ApprenticeCodex.MODID, "feather_rush", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "slash_blade", SpellDispenserSpellProfile.OWNER_OPTIONAL_UP),
                        profile(ApprenticeCodex.MODID, "precision_jack", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "auto_turret", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "search_beacon", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "thermal_process", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "mage_light", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "force_field", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "illuminate_stellar", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "unite_luna", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "sky_edge", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "breaching_enemy", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "bullet_stream", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "fly_swatter", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "shock", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "compound_phial", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "tiny_lumberjack", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "graced_rain", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "world_flatter", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "earth_forge", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "grind_runner", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "harvest_moon", SpellDispenserSpellProfile.DEFAULT),
                        profile(ApprenticeCodex.MODID, "moon_light", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "tiro_volley", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "silent_assassin", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "magic_spear", SpellDispenserSpellProfile.OWNER_OPTIONAL),
                        profile(ApprenticeCodex.MODID, "frost_rune", SpellDispenserSpellProfile.OWNER_OPTIONAL)
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
