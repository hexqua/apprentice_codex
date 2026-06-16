package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightChargedTwinBladeStaffCompat {
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "charged_twin_blade_staff");
    private static final ResourceLocation SPEAR_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(EpicFightCompat.MOD_ID, "spear");

    private EpicFightChargedTwinBladeStaffCompat() {
    }

    public static void register(IEventBus modEventBus) {
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(
                EpicFightChargedTwinBladeStaffCompat::onWeaponCapabilityPresetRegistry,
                "apprenticecodex:charged_twin_blade_staff"
        );
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, EpicFightChargedTwinBladeStaffCompat::buildCapability);
    }

    private static CapabilityItem.Builder<?> buildCapability(Item item) {
        var spearFactory = WeaponTypeReloadListener.get(SPEAR_TYPE_ID);
        var baseBuilder = spearFactory != null ? spearFactory.apply(item) : null;
        var builder = baseBuilder instanceof WeaponCapability.Builder weaponBuilder
                ? weaponBuilder
                : WeaponCapability.builder();

        builder.zoomInType(CapabilityItem.ZoomInType.USE_TICK);
        builder.constructor(EpicFightChargedTwinBladeStaffCapability::new);
        // spear category にするとガードスキル側の対応カテゴリへ混ざるため、投擲武器として扱う。
        builder.category(CapabilityItem.WeaponCategories.TRIDENT);

        return builder;
    }
}
