package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileDefinition;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileList;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerDirectionMode;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerOriginMode;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RemoteOwnerCastSpellProfileDataGenerator extends JsonCodecProvider<RemoteOwnerCastProfileList> {
    public RemoteOwnerCastSpellProfileDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                RemoteOwnerCastProfileManager.DIRECTORY,
                RemoteOwnerCastProfileList.CODEC,
                Map.of(
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "profiles"),
                        new RemoteOwnerCastProfileList(createProfileDefinitions())
                )
        );
    }

    public static List<RemoteOwnerCastProfileDefinition> createProfileDefinitions() {
        var profiles = new LinkedHashMap<ResourceLocation, RemoteOwnerCastProfile>();

        putProfiles(profiles, RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY,
                SpellRegistry.ACUPUNCTURE_SPELL,
                SpellRegistry.BLOOD_NEEDLES_SPELL,
                SpellRegistry.BLOOD_SLASH_SPELL,
                SpellRegistry.DEVOUR_SPELL,
                SpellRegistry.WITHER_SKULL_SPELL,
                SpellRegistry.SCULK_TENTACLES_SPELL,
                SpellRegistry.SONIC_BOOM_SPELL,
                SpellRegistry.BLACK_HOLE_SPELL,
                SpellRegistry.MAGIC_ARROW_SPELL,
                SpellRegistry.MAGIC_MISSILE_SPELL,
                SpellRegistry.SHADOW_SLASH,
                SpellRegistry.STARFALL_SPELL,
                SpellRegistry.ARROW_VOLLEY_SPELL,
                SpellRegistry.CHAIN_CREEPER_SPELL,
                SpellRegistry.FANG_STRIKE_SPELL,
                SpellRegistry.FANG_WARD_SPELL,
                SpellRegistry.FIRECRACKER_SPELL,
                SpellRegistry.GUST_SPELL,
                SpellRegistry.LOB_CREEPER_SPELL,
                SpellRegistry.SHIELD_SPELL,
                SpellRegistry.SLOW_SPELL,
                SpellRegistry.SPECTRAL_HAMMER_SPELL,
                SpellRegistry.BLAZE_STORM_SPELL,
                SpellRegistry.FIRE_ARROW_SPELL,
                SpellRegistry.FIREBALL_SPELL,
                SpellRegistry.FIREBOLT_SPELL,
                SpellRegistry.FLAMING_STRIKE_SPELL,
                SpellRegistry.MAGMA_BOMB_SPELL,
                SpellRegistry.SCORCH_SPELL,
                SpellRegistry.BLESSING_OF_LIFE_SPELL,
                SpellRegistry.DIVINE_SMITE_SPELL,
                SpellRegistry.GUIDING_BOLT_SPELL,
                SpellRegistry.HEALING_CIRCLE_SPELL,
                SpellRegistry.SUNBEAM_SPELL,
                SpellRegistry.WISP_SPELL,
                SpellRegistry.FROSTWAVE_SPELL,
                SpellRegistry.ICE_BLOCK_SPELL,
                SpellRegistry.ICE_SPIKES_SPELL,
                SpellRegistry.ICICLE_SPELL,
                SpellRegistry.RAY_OF_FROST_SPELL,
                SpellRegistry.SNOWBALL_SPELL,
                SpellRegistry.BALL_LIGHTNING_SPELL,
                SpellRegistry.CHAIN_LIGHTNING_SPELL,
                SpellRegistry.LIGHTNING_BOLT_SPELL,
                SpellRegistry.LIGHTNING_LANCE_SPELL,
                SpellRegistry.ACID_ORB_SPELL,
                SpellRegistry.BLIGHT_SPELL,
                SpellRegistry.EARTHQUAKE_SPELL,
                SpellRegistry.FIREFLY_SWARM_SPELL,
                SpellRegistry.POISON_ARROW_SPELL,
                SpellRegistry.POISON_SPLASH_SPELL,
                SpellRegistry.ROOT_SPELL,
                SpellRegistry.STOMP_SPELL,
                SpellRegistry.TOUCH_DIG);

        putProfiles(profiles, RemoteOwnerCastProfile.REMOTE_PLAYER_GEOMETRY,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BLAST,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCANE_BEAM,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.AUTO_TURRET,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.FORCE_FIELD,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.ILLUMINATE_STELLAR,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.UNITE_LUNA,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.SKY_EDGE,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHOCK,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.CATCH_FLAME,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPOUND_PHIAL,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.EARTH_FORGE,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.HARVEST_MOON,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.TIRO_VOLLEY,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGIC_SPEAR,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.FROST_RUNE,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.INSCRIBE_ICE);

        putProfiles(profiles, remoteAnchorOwnerProfile(false),
                SpellRegistry.DRAGON_BREATH_SPELL,
                SpellRegistry.FIRE_BREATH_SPELL,
                SpellRegistry.CONE_OF_COLD_SPELL,
                SpellRegistry.ELECTROCUTE_SPELL,
                SpellRegistry.POISON_BREATH_SPELL);

        putProfiles(profiles, remoteAnchorOwnerProfile(false),
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.SLASH_BLADE,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.SHIDEN,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.PRECISION_JACK,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.THERMAL_PROCESS,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.BREACHING_ENEMY,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.BULLET_STREAM,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.FLY_SWATTER,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.TINY_LUMBERJACK,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.GRACED_RAIN,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.WORLD_FLATTER,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.GRIND_RUNNER,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.MOON_LIGHT,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.SILENT_ASSASSIN,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.LETHAL_ASSAULT,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.DUAL_ACROBAT,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARTISAN_SMASH,
                jp.aquafactory.apprenticecodex.registry.SpellRegistry.FUJIN);

        putProfile(profiles, SpellRegistry.BLOOD_STEP_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.HEARTSTOP_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.RAISE_DEAD_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.PLANAR_SIGHT_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.COUNTERSPELL_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.ECHOING_STRIKES_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.EVASION_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.SUMMON_SWORDS, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.INVISIBILITY_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.SUMMON_HORSE_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.SUMMON_VEX_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.BURNING_DASH_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.HEAT_SURGE_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.ANGEL_WINGS_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.CLEANSE_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.FORTIFY_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.GREATER_HEAL_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.HASTE_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.HEAL_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.ICE_TOMB_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.SUMMON_POLAR_BEAR_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.ASCENSION_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.CHARGE_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.SHOCKWAVE_SPELL, remoteGeometryProfile(true));
        putProfile(profiles, SpellRegistry.VOLT_STRIKE_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.GLUTTONY_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.OAKSKIN_SPELL, playerSelfProfile(false));
        putProfile(profiles, SpellRegistry.SPIDER_ASPECT_SPELL, playerSelfProfile(false));

        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.ARCHER_MULTIPLE, remoteGeometryProfile(true));
        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.HIGANBANA, remoteGeometryProfile(false));
        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.AUTO_MAGNET, playerSelfProfile(false));
        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPANION_TRUNK, playerSelfProfile(false));
        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SENSE_EVIL, playerSelfProfile(false));
        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMMENCE_FIRE, remoteGeometryProfile(true));
        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.DEEP_SENSOR, playerSelfProfile(false));
        putProfile(profiles, jp.aquafactory.apprenticecodex.registry.SpellRegistry.SPECTRAL_WING, playerSelfProfile(false));

        return profiles.entrySet().stream()
                .map(entry -> new RemoteOwnerCastProfileDefinition(entry.getKey(), entry.getValue()))
                .toList();
    }

    @SafeVarargs
    private static void putProfiles(
            Map<ResourceLocation, RemoteOwnerCastProfile> profiles,
            RemoteOwnerCastProfile profile,
            RegistryObject<? extends AbstractSpell>... spells
    ) {
        for (var spell : spells) {
            putProfile(profiles, spell, profile);
        }
    }

    private static void putProfile(
            Map<ResourceLocation, RemoteOwnerCastProfile> profiles,
            RegistryObject<? extends AbstractSpell> spell,
            RemoteOwnerCastProfile profile
    ) {
        profiles.put(getResourceLocationRegistry(spell), profile);
    }

    private static RemoteOwnerCastProfile playerSelfProfile(boolean allowInitialRecast) {
        return new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.PLAYER_SELF,
                RemoteOwnerOriginMode.PLAYER_SELF,
                RemoteOwnerDirectionMode.PLAYER_LOOK,
                Optional.of(List.of(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)),
                allowInitialRecast
        );
    }

    private static RemoteOwnerCastProfile remoteGeometryProfile(boolean allowInitialRecast) {
        return new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY,
                RemoteOwnerOriginMode.PROVIDED_ORIGIN,
                RemoteOwnerDirectionMode.PROVIDED_FORWARD,
                Optional.of(List.of(RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT)),
                allowInitialRecast
        );
    }

    private static RemoteOwnerCastProfile remoteAnchorOwnerProfile(boolean allowInitialRecast) {
        return new RemoteOwnerCastProfile(
                RemoteOwnerCastMode.REMOTE_ANCHOR_OWNER_MAGIC,
                RemoteOwnerOriginMode.PROVIDED_ORIGIN,
                RemoteOwnerDirectionMode.PROVIDED_FORWARD,
                Optional.empty(),
                allowInitialRecast
        );
    }

    private static ResourceLocation getResourceLocationRegistry(RegistryObject<? extends AbstractSpell> spellRegistryObject) {
        return ResourceLocation.fromNamespaceAndPath(
                Objects.requireNonNull(spellRegistryObject.getId()).getNamespace(),
                Objects.requireNonNull(spellRegistryObject.getId()).getPath()
        );
    }
}
