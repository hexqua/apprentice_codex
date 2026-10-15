package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

// 共通コードは反射登録のみを行い、Epic Fight未導入時にはこのクラスを読み込まない。
@SuppressWarnings("unused")
public final class EpicFightPsychicDriveGauntletsCompat {
    private EpicFightPsychicDriveGauntletsCompat() {}
    public static void register(IEventBus modEventBus) {
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(
                EpicFightPsychicDriveGauntletsCompat::presets, "apprenticecodex:psychic_drive_gauntlets");
        EpicFightEventHooks.Player.CAST_SKILL.registerEvent(
                EpicFightPsychicDriveGauntletsCompat::cancelBasicAttack, "apprenticecodex:psychic_drive_gauntlets_attack");
    }
    private static void presets(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "psychic_drive_gauntlets"),
                item -> RangedWeaponCapability.builder().zoomInType(CapabilityItem.ZoomInType.NONE));
    }
    public static void cancelBasicAttack(SkillCastEvent event) {
        if (PsychicDriveGauntletsSelection.isGauntlets(event.getPlayerPatch().getOriginal().getMainHandItem())
                && event.getSkillContainer().getSlot() == SkillSlots.COMBO_ATTACKS) {
            event.cancel();
        }
    }
}
