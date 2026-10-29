package jp.aquafactory.apprenticecodex.compat.epicfight;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

// 共通コードは反射登録のみを行い、Epic Fight未導入時にはこのクラスを読み込まない。
@SuppressWarnings("unused")
public final class EpicFightPsychicDriveGauntletsCompat {
    private EpicFightPsychicDriveGauntletsCompat() {}
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightPsychicDriveGauntletsCompat::presets);
        MinecraftForge.EVENT_BUS.addListener(EpicFightPsychicDriveGauntletsCompat::tick);
    }

    private static final UUID ATTACK_LISTENER = UUID.fromString("b70da229-f680-4f99-bff1-dddf973127c3");
    private static final Set<ServerPlayerPatch> INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());

    private static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) install(player);
    }

    public static void install(ServerPlayer player) {
        var patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        // Forge版は全player共通のSkillCastフックを持たない。patch単位で登録し、再生成時にも付け直す。
        if (patch != null && INSTALLED.add(patch)) {
            patch.getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, ATTACK_LISTENER,
                    EpicFightPsychicDriveGauntletsCompat::cancelBasicAttack);
        }
    }

    private static void presets(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "psychic_drive_gauntlets"),
                item -> RangedWeaponCapability.builder().zoomInType(CapabilityItem.ZoomInType.NONE));
    }
    public static void cancelBasicAttack(SkillCastEvent event) {
        if (PsychicDriveGauntletsSelection.isGauntlets(event.getPlayerPatch().getOriginal().getMainHandItem())
                && event.getSkillContainer().getSlot() == SkillSlots.BASIC_ATTACK) {
            event.setCanceled(true);
        }
    }
}
