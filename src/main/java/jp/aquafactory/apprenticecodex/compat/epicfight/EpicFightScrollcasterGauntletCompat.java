package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightScrollcasterGauntletCompat {
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "scrollcaster_gauntlet");
    private static final ResourceLocation FIST_PRESET_ID =
            ResourceLocation.fromNamespaceAndPath(EpicFightCompat.MOD_ID, "fist");

    private EpicFightScrollcasterGauntletCompat() {
    }

    public static void register(IEventBus modEventBus) {
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(
                EpicFightScrollcasterGauntletCompat::onWeaponCapabilityPresetRegistry,
                "apprenticecodex:scrollcaster_gauntlet"
        );
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(WEAPON_TYPE_ID, item -> buildCapability(event, item));
    }

    private static CapabilityItem.Builder<?> buildCapability(WeaponCapabilityPresetRegistryEvent event, Item item) {
        var fistFactory = event.getTypeEntry().get(FIST_PRESET_ID);
        var builder = fistFactory != null
                ? (WeaponCapability.Builder) fistFactory.apply(item)
                : WeaponCapability.builder();

        builder.constructor(EpicFightScrollcasterGauntletCapability::new);

        return builder;
    }
}
