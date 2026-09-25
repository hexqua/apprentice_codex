package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

// リフレクションで参照するため、IDE側の未使用検知を無効化.
@SuppressWarnings("unused")
public final class EpicFightFullautoRapidcastSpellrifleCompat {
    public static final ResourceLocation WEAPON_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "fullauto_rapidcast_spellrifle");
    private static final ResourceLocation CROSSBOW_PRESET_ID =
            ResourceLocation.fromNamespaceAndPath(EpicFightCompat.MOD_ID, "crossbow");

    private EpicFightFullautoRapidcastSpellrifleCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightFullautoRapidcastSpellrifleCompat::onWeaponCapabilityPresetRegistry);
    }

    private static void onWeaponCapabilityPresetRegistry(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(
                WEAPON_TYPE_ID,
                item -> EpicFightFullautoRapidcastSpellrifleCompat.buildCapability(item, CROSSBOW_PRESET_ID)
        );
    }

    private static CapabilityItem.Builder buildCapability(Item item, ResourceLocation basePresetId) {
        var builder = (RangedWeaponCapability.Builder) WeaponTypeReloadListener.getOrThrow(basePresetId.toString()).apply(item);
        builder.constructor(EpicFightFullautoRapidcastSpellrifleCapability::new);
        return builder;
    }
}
