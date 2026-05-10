package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCapabilityPresets;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightChargedTwinBladeStaffCompat {
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "charged_twin_blade_staff");

    private EpicFightChargedTwinBladeStaffCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightChargedTwinBladeStaffCompat::onWeaponCapabilityPresetRegistry);
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, EpicFightChargedTwinBladeStaffCompat::buildCapability);
    }

    private static CapabilityItem.Builder buildCapability(Item item) {
        var builder = (WeaponCapability.Builder) WeaponCapabilityPresets.SPEAR.apply(item);

        builder.zoomInType(CapabilityItem.ZoomInType.USE_TICK);
        builder.constructor(EpicFightChargedTwinBladeStaffCapability::new);
        // spear category にするとガードスキル側の対応カテゴリへ混ざるため、投擲武器として扱う。
        builder.category(CapabilityItem.WeaponCategories.TRIDENT);

        return builder;
    }
}
