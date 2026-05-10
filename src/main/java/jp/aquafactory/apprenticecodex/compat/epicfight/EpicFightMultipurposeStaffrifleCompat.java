package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCapabilityPresets;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightMultipurposeStaffrifleCompat {
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "multipurpose_staffrifle");

    private EpicFightMultipurposeStaffrifleCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightMultipurposeStaffrifleCompat::onWeaponCapabilityPresetRegistry);
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, EpicFightMultipurposeStaffrifleCompat::buildCapability);
    }

    private static CapabilityItem.Builder buildCapability(Item item) {
        var builder = (RangedWeaponCapability.Builder) WeaponCapabilityPresets.CROSSBOW.apply(item);
        builder.constructor(EpicFightMultipurposeStaffrifleCapability::new);
        return builder;
    }
}
