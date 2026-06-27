package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, ApprenticeCodex.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String id) {
        return SOUND_EVENTS.register(id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, id)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE = reg("rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUPPRESS_RIFLE = reg("suppress_rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDGUN = reg("handgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN = reg("shotgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSKET = reg("musket");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_SINGLE = reg("minigun_single");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_LOOP = reg("minigun_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_WARMUP = reg("minigun_warmup");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_FINISH = reg("minigun_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARCANE_BLAST = reg("arcane_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_START = reg("saw_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_STOP = reg("saw_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOUD_RAIN = reg("cloud_rain");
    public static final DeferredHolder<SoundEvent, SoundEvent> SET_MAGE_LIGHT_TORCH = reg("set_mage_light");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAPPED = reg("flapped");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS = reg("mantis");
    public static final DeferredHolder<SoundEvent, SoundEvent> PALETTE = reg("palette");
    public static final DeferredHolder<SoundEvent, SoundEvent> ABSORB = reg("absorb");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHALANX = reg("phalanx");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_FIELD = reg("force_field");
    public static final DeferredHolder<SoundEvent, SoundEvent> THRUST = reg("thrust");
    public static final DeferredHolder<SoundEvent, SoundEvent> THIRST_DRAIN = reg("thirst_drain");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLASH_DRAIN = reg("slash_drain");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERRAIN = reg("terrain");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_FIELD_DEFLECT = reg("force_field_deflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_FIELD_ACTIVATE = reg("force_field_activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> KATANA_SLASH = reg("katana_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOON_LIGHT_DIMENSION = reg("moon_light_dimension");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_LAUNCH = reg("wheel_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_PROCESS = reg("wheel_process");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHEEL_SPINUP = reg("wheel_spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> REMOTE_PREPARE = reg("remote_prepare");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENSE_EVIL = reg("sense_evil");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIPHON_ORB_LAUNCH = reg("siphon_orb_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANA_SLASH = reg("mana_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY = reg("parry");
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_FIRE = reg("stellar_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_LAUNCH = reg("stellar_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_IMPACT = reg("stellar_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> STELLAR_EXPLODE = reg("stellar_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_RUNE = reg("frozen_rune");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAFFRIFLE = reg("staffrifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MULTICAST = reg("multicast");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYSTIC_SHIELD_DEPLOY = reg("mystic_shield_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYSTIC_SHIELD_BLOCK = reg("mystic_shield_block");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYSTIC_SHIELD_SHOOT = reg("mystic_shield_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIST_FORM_START = reg("mist_form_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIST_FORM_FINISHED = reg("mist_form_finished");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_DAGGER_THROW = reg("ice_dagger_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_DAGGER_HIT = reg("ice_dagger_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAMI = reg("kami");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVE = reg("revolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMETHYST_FIST = reg("amethyst_fist");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANA_JET = reg("mana_jet");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRAGE = reg("mirage");
    public static final DeferredHolder<SoundEvent, SoundEvent> FULLAUTO_RIFLE = reg("fullauto_rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMG = reg("smg");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELLCHARGE = reg("spellcharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMASHCAST_SCEPTER_SMASH_AIR = reg("smashcast_scepter_smash_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMASHCAST_SCEPTER_SMASH_GROUND = reg("smashcast_scepter_smash_ground");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMASHCAST_SCEPTER_SMASH_GROUND_HEAVY = reg("smashcast_scepter_smash_ground_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_SUMMON_WEAPON = reg("vanilla_summon_weapon");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_SUMMON_MAGICAL_ENTITY = reg("vanilla_summon_magical_entity");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_DISAPPEAR_MAGICAL_ENTITY = reg("vanilla_disappear_magical_entity");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_RIFT_HOLE = reg("vanilla_rift_hole");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_PROJECTILE_SHOOT = reg("vanilla_projectile_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_SUMMON_TRUNK = reg("vanilla_summon_trunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_HIGH_JUMP = reg("vanilla_high_jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_BRAZIER_SACRIFICE = reg("vanilla_brazier_sacrifice");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_BREAK_DOOR = reg("vanilla_break_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_LONG_STRIDE = reg("vanilla_long_stride");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_HOLD_WEAPON = reg("vanilla_hold_weapon");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_ARMOR_EQUIP_ROBE = reg("vanilla_armor_equip_robe");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_POWER_ACTIVATE = reg("vanilla_power_activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_POWER_TUNING = reg("vanilla_power_tuning");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_INTERFACE_OPEN = reg("vanilla_interface_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_START_SEARCH = reg("vanilla_start_search");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_CONSTRUCTING_MECHANICAL = reg("vanilla_constructing_mechanical");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_CONSTRUCTED_MECHANICAL = reg("vanilla_constructed_mechanical");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_CRITICAL_SHOT = reg("vanilla_critical_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_USE_DESK = reg("vanilla_use_desk");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_INSCRIBE_MANA = reg("vanilla_inscribe_mana");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_DEMICREATOR_BREAK = reg("vanilla_demicreator_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_FEATHER_HIT = reg("vanilla_feather_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_FEATHER_SHOOT = reg("vanilla_feather_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_JAR_CLOSE = reg("vanilla_jar_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_JAR_OPEN = reg("vanilla_jar_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_CHEST_JUMP = reg("vanilla_chest_jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_CARD_THROW = reg("vanilla_card_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> VANILLA_FEED_AMMO = reg("vanilla_feed_ammo");

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
