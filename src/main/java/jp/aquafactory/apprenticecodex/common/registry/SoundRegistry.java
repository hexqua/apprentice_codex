package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ApprenticeCodex.MODID);

    public static final RegistryObject<SoundEvent> RIFLE =
            SOUND_EVENTS.register("rifle",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "rifle")));

    public static final RegistryObject<SoundEvent> HANDGUN =
            SOUND_EVENTS.register("handgun",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "handgun")));

    public static final RegistryObject<SoundEvent> SHOTGUN =
            SOUND_EVENTS.register("shotgun",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "shotgun")));

    public static final RegistryObject<SoundEvent> MINIGUN_SINGLE =
            SOUND_EVENTS.register("minigun_single",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_single")));

    public static final RegistryObject<SoundEvent> MINIGUN_LOOP =
            SOUND_EVENTS.register("minigun_loop",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_loop")));

    public static final RegistryObject<SoundEvent> MINIGUN_WARMUP =
            SOUND_EVENTS.register("minigun_warmup",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_warmup")));

    public static final RegistryObject<SoundEvent> MINIGUN_FINISH =
            SOUND_EVENTS.register("minigun_finish",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_finish")));

    public static final RegistryObject<SoundEvent> ARCANE_BLAST =
            SOUND_EVENTS.register("arcane_blast",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcane_blast")));

    public static final RegistryObject<SoundEvent> SAW_START =
            SOUND_EVENTS.register("saw_start",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "saw_start")));

    public static final RegistryObject<SoundEvent> SAW_STOP =
            SOUND_EVENTS.register("saw_stop",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "saw_stop")));

    public static final RegistryObject<SoundEvent> CLOUD_RAIN =
            SOUND_EVENTS.register("cloud_rain",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "cloud_rain")));

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
