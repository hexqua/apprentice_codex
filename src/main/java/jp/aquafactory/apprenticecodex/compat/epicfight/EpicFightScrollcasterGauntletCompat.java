package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

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
        modEventBus.addListener(EpicFightScrollcasterGauntletCompat::onWeaponCapabilityPresetRegistry);
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(
                WEAPON_TYPE_ID,
                item -> EpicFightScrollcasterGauntletCompat.buildCapability(item, FIST_PRESET_ID)
        );
    }

    private static CapabilityItem.Builder buildCapability(Item item, ResourceLocation basePresetId) {
        var builder = (WeaponCapability.Builder) WeaponTypeReloadListener.getOrThrow(basePresetId.toString()).apply(item);

        builder.constructor(EpicFightScrollcasterGauntletCapability::new);

        return builder;
    }
}
