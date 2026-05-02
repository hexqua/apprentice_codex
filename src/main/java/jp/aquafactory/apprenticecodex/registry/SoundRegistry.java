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
    public static final RegistryObject<SoundEvent> MINIGUN_SINGLE = reg("minigun_single");
    public static final RegistryObject<SoundEvent> MINIGUN_LOOP = reg("minigun_loop");
    public static final RegistryObject<SoundEvent> MINIGUN_WARMUP = reg("minigun_warmup");
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


    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
