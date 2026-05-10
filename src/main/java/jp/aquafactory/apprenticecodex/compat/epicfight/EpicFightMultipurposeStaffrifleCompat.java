package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightMultipurposeStaffrifleCompat {
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "multipurpose_staffrifle");

    private EpicFightMultipurposeStaffrifleCompat() {
    }

    public static void register(IEventBus modEventBus) {
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(
                EpicFightMultipurposeStaffrifleCompat::onWeaponCapabilityPresetRegistry,
                "apprenticecodex:multipurpose_staffrifle"
        );
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, EpicFightMultipurposeStaffrifleCompat::buildCapability);
    }

    private static RangedWeaponCapability.Builder buildCapability(Item item) {
        return RangedWeaponCapability.builder()
                .constructor(EpicFightMultipurposeStaffrifleCapability::new);
    }
}
