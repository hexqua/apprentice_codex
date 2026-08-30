package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ApprenticeCodex.MODID);

    private static RegistryObject<SoundEvent> reg(String id) {
        return SOUND_EVENTS.register(id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, id)));
    }

    public static final RegistryObject<SoundEvent> RIFLE = reg("rifle");
    public static final RegistryObject<SoundEvent> SUPPRESS_RIFLE = reg("suppress_rifle");
    public static final RegistryObject<SoundEvent> HANDGUN = reg("handgun");
    public static final RegistryObject<SoundEvent> SHOTGUN = reg("shotgun");
    public static final RegistryObject<SoundEvent> MUSKET = reg("musket");
    public static final RegistryObject<SoundEvent> MINIGUN_WARMUP = reg("minigun_warmup");
    public static final RegistryObject<SoundEvent> MINIGUN_FIRING = reg("minigun_firing");
    public static final RegistryObject<SoundEvent> MINIGUN_FINISH = reg("minigun_finish");
    public static final RegistryObject<SoundEvent> ARCANE_BLAST = reg("arcane_blast");
    public static final RegistryObject<SoundEvent> SAW_START = reg("saw_start");
    public static final RegistryObject<SoundEvent> SAW_STOP = reg("saw_stop");
    public static final RegistryObject<SoundEvent> CLOUD_RAIN = reg("cloud_rain");
    public static final RegistryObject<SoundEvent> SET_MAGE_LIGHT_TORCH = reg("set_mage_light");
    public static final RegistryObject<SoundEvent> FLAPPED = reg("flapped");
    public static final RegistryObject<SoundEvent> MANTIS = reg("mantis");
    public static final RegistryObject<SoundEvent> PALETTE = reg("palette");
    public static final RegistryObject<SoundEvent> ABSORB = reg("absorb");
    public static final RegistryObject<SoundEvent> PHALANX = reg("phalanx");
    public static final RegistryObject<SoundEvent> FORCE_FIELD = reg("force_field");
    public static final RegistryObject<SoundEvent> THRUST = reg("thrust");
    public static final RegistryObject<SoundEvent> THIRST_DRAIN = reg("thirst_drain");
    public static final RegistryObject<SoundEvent> SLASH_DRAIN = reg("slash_drain");
    public static final RegistryObject<SoundEvent> TERRAIN = reg("terrain");
    public static final RegistryObject<SoundEvent> FORCE_FIELD_DEFLECT = reg("force_field_deflect");
    public static final RegistryObject<SoundEvent> FORCE_FIELD_ACTIVATE = reg("force_field_activate");
    public static final RegistryObject<SoundEvent> KATANA_SLASH = reg("katana_slash");
    public static final RegistryObject<SoundEvent> MOON_LIGHT_DIMENSION = reg("moon_light_dimension");
    public static final RegistryObject<SoundEvent> WHEEL_LAUNCH = reg("wheel_launch");
    public static final RegistryObject<SoundEvent> WHEEL_PROCESS = reg("wheel_process");
    public static final RegistryObject<SoundEvent> WHEEL_SPINUP = reg("wheel_spinup");
    public static final RegistryObject<SoundEvent> REMOTE_PREPARE = reg("remote_prepare");
    public static final RegistryObject<SoundEvent> SENSE_EVIL = reg("sense_evil");
    public static final RegistryObject<SoundEvent> SIPHON_ORB_LAUNCH = reg("siphon_orb_launch");
    public static final RegistryObject<SoundEvent> MANA_SLASH = reg("mana_slash");
    public static final RegistryObject<SoundEvent> PARRY = reg("parry");
    public static final RegistryObject<SoundEvent> STELLAR_FIRE = reg("stellar_fire");
    public static final RegistryObject<SoundEvent> STELLAR_LAUNCH = reg("stellar_launch");
    public static final RegistryObject<SoundEvent> STELLAR_IMPACT = reg("stellar_impact");
    public static final RegistryObject<SoundEvent> STELLAR_EXPLODE = reg("stellar_explode");
    public static final RegistryObject<SoundEvent> FROZEN_RUNE = reg("frozen_rune");
    public static final RegistryObject<SoundEvent> STAFFRIFLE = reg("staffrifle");
    public static final RegistryObject<SoundEvent> MULTICAST = reg("multicast");
    public static final RegistryObject<SoundEvent> MYSTIC_SHIELD_DEPLOY = reg("mystic_shield_deploy");
    public static final RegistryObject<SoundEvent> MYSTIC_SHIELD_BLOCK = reg("mystic_shield_block");
    public static final RegistryObject<SoundEvent> MYSTIC_SHIELD_SHOOT = reg("mystic_shield_shoot");
    public static final RegistryObject<SoundEvent> MIST_FORM_START = reg("mist_form_start");
    public static final RegistryObject<SoundEvent> MIST_FORM_FINISHED = reg("mist_form_finished");
    public static final RegistryObject<SoundEvent> ICE_DAGGER_THROW = reg("ice_dagger_throw");
    public static final RegistryObject<SoundEvent> ICE_DAGGER_HIT = reg("ice_dagger_hit");
    public static final RegistryObject<SoundEvent> KAMI = reg("kami");
    public static final RegistryObject<SoundEvent> REVOLVE = reg("revolve");
    public static final RegistryObject<SoundEvent> AMETHYST_FIST = reg("amethyst_fist");
    public static final RegistryObject<SoundEvent> MANA_JET = reg("mana_jet");
    public static final RegistryObject<SoundEvent> BROOM_ACCELERATE = reg("broom_accelerate");
    public static final RegistryObject<SoundEvent> MIRAGE = reg("mirage");
    public static final RegistryObject<SoundEvent> FULLAUTO_RIFLE = reg("fullauto_rifle");
    public static final RegistryObject<SoundEvent> SMG = reg("smg");
    public static final RegistryObject<SoundEvent> SPELLCHARGE = reg("spellcharge");
    public static final RegistryObject<SoundEvent> SHIDEN = reg("shiden");

    public static final RegistryObject<SoundEvent> SMASHCAST_SCEPTER_SMASH_AIR = reg("smashcast_scepter_smash_air");
    public static final RegistryObject<SoundEvent> SMASHCAST_SCEPTER_SMASH_GROUND = reg("smashcast_scepter_smash_ground");
    public static final RegistryObject<SoundEvent> SMASHCAST_SCEPTER_SMASH_GROUND_HEAVY = reg("smashcast_scepter_smash_ground_heavy");

    public static final RegistryObject<SoundEvent> VANILLA_SUMMON_WEAPON = reg("vanilla_summon_weapon");
    public static final RegistryObject<SoundEvent> VANILLA_SUMMON_MAGICAL_ENTITY = reg("vanilla_summon_magical_entity");
    public static final RegistryObject<SoundEvent> VANILLA_DISAPPEAR_MAGICAL_ENTITY = reg("vanilla_disappear_magical_entity");
    public static final RegistryObject<SoundEvent> VANILLA_RIFT_HOLE = reg("vanilla_rift_hole");
    public static final RegistryObject<SoundEvent> VANILLA_PROJECTILE_SHOOT = reg("vanilla_projectile_shoot");
    public static final RegistryObject<SoundEvent> VANILLA_SUMMON_TRUNK = reg("vanilla_summon_trunk");
    public static final RegistryObject<SoundEvent> VANILLA_HIGH_JUMP = reg("vanilla_high_jump");
    public static final RegistryObject<SoundEvent> VANILLA_BRAZIER_SACRIFICE = reg("vanilla_brazier_sacrifice");
    public static final RegistryObject<SoundEvent> VANILLA_BREAK_DOOR = reg("vanilla_break_door");
    public static final RegistryObject<SoundEvent> VANILLA_LONG_STRIDE = reg("vanilla_long_stride");
    public static final RegistryObject<SoundEvent> VANILLA_HOLD_WEAPON = reg("vanilla_hold_weapon");
    public static final RegistryObject<SoundEvent> VANILLA_ARMOR_EQUIP_ROBE = reg("vanilla_armor_equip_robe");
    public static final RegistryObject<SoundEvent> VANILLA_POWER_ACTIVATE = reg("vanilla_power_activate");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_RECONSTRUCT = reg("vanilla_broom_reconstruct");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_DAMAGE = reg("vanilla_broom_damage");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_CRITICAL_DAMAGE = reg("vanilla_broom_critical_damage");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_WARNING = reg("vanilla_broom_warning");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_EMERGENCY = reg("vanilla_broom_emergency");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_MOUNT_REJECT = reg("vanilla_broom_mount_reject");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_PROPULSION_LOST = reg("vanilla_broom_propulsion_lost");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_PROPULSION_RECOVERED = reg("vanilla_broom_propulsion_recovered");
    public static final RegistryObject<SoundEvent> VANILLA_BROOM_IMPULSE = reg("vanilla_broom_impulse");
    public static final RegistryObject<SoundEvent> VANILLA_POWER_TUNING = reg("vanilla_power_tuning");
    public static final RegistryObject<SoundEvent> VANILLA_INTERFACE_OPEN = reg("vanilla_interface_open");
    public static final RegistryObject<SoundEvent> VANILLA_START_SEARCH = reg("vanilla_start_search");
    public static final RegistryObject<SoundEvent> VANILLA_CONSTRUCTING_MECHANICAL = reg("vanilla_constructing_mechanical");
    public static final RegistryObject<SoundEvent> VANILLA_CONSTRUCTED_MECHANICAL = reg("vanilla_constructed_mechanical");
    public static final RegistryObject<SoundEvent> VANILLA_CRITICAL_SHOT = reg("vanilla_critical_shot");
    public static final RegistryObject<SoundEvent> VANILLA_USE_DESK = reg("vanilla_use_desk");
    public static final RegistryObject<SoundEvent> VANILLA_INSCRIBE_MANA = reg("vanilla_inscribe_mana");
    public static final RegistryObject<SoundEvent> VANILLA_DEMICREATOR_BREAK = reg("vanilla_demicreator_break");
    public static final RegistryObject<SoundEvent> VANILLA_FEATHER_HIT = reg("vanilla_feather_hit");
    public static final RegistryObject<SoundEvent> VANILLA_FEATHER_SHOOT = reg("vanilla_feather_shoot");
    public static final RegistryObject<SoundEvent> VANILLA_JAR_CLOSE = reg("vanilla_jar_close");
    public static final RegistryObject<SoundEvent> VANILLA_JAR_OPEN = reg("vanilla_jar_open");
    public static final RegistryObject<SoundEvent> VANILLA_CHEST_JUMP = reg("vanilla_chest_jump");
    public static final RegistryObject<SoundEvent> VANILLA_CARD_THROW = reg("vanilla_card_throw");
    public static final RegistryObject<SoundEvent> VANILLA_FEED_AMMO = reg("vanilla_feed_ammo");
    public static final RegistryObject<SoundEvent> VANILLA_CAST_BOOK = reg("vanilla_cast_book");
    public static final RegistryObject<SoundEvent> VANILLA_CRYSTALLIZE_MANA = reg("vanilla_crystallize_mana");
    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
