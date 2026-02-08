package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ApprenticeCodex.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE =
            SOUND_EVENTS.register("rifle",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "rifle")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HANDGUN =
            SOUND_EVENTS.register("handgun",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "handgun")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN =
            SOUND_EVENTS.register("shotgun",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "shotgun")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_SINGLE =
            SOUND_EVENTS.register("minigun_single",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_single")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_LOOP =
            SOUND_EVENTS.register("minigun_loop",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_loop")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_WARMUP =
            SOUND_EVENTS.register("minigun_warmup",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_warmup")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_FINISH =
            SOUND_EVENTS.register("minigun_finish",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "minigun_finish")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ARCANE_BLAST =
            SOUND_EVENTS.register("arcane_blast",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcane_blast")));

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
