package jp.aquafactory.apprenticecodex.common.capability;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.capability.personalinventory.PersonalInventoryProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public class CapabilityEvents {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PersonalInventoryProvider.ID, new PersonalInventoryProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();

        // パーソナルシェルフ:死亡時も引き継ぎが必要.
        event.getOriginal().getCapability(Capabilities.PERSONAL_INVENTORY).ifPresent(oldInventory -> {
            event.getEntity().getCapability(Capabilities.PERSONAL_INVENTORY).ifPresent(newInventory -> {
                // NBTコピーで対応.
                newInventory.deserializeNBT(oldInventory.serializeNBT());
            });
        });

        event.getOriginal().invalidateCaps();
    }
}
