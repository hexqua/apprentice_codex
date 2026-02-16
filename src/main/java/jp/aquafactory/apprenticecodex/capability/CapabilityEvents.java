package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventoryProvider;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellDataProvider;
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
            event.addCapability(CodexSpellDataProvider.ID, new CodexSpellDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();

        // パーソナルシェルフ:死亡時も引き継ぎが必要.
        event.getOriginal().getCapability(Capabilities.PERSONAL_INVENTORY).ifPresent(
                oldInventory -> event.getEntity().getCapability(Capabilities.PERSONAL_INVENTORY).ifPresent(
                        newInventory -> newInventory.deserializeNBT(oldInventory.serializeNBT())
                )
        );

        // 魔法データ:死亡時も引き継ぐ.
        event.getOriginal().getCapability(Capabilities.SPELL_DATA).ifPresent(
                oldSpellData -> event.getEntity().getCapability(Capabilities.SPELL_DATA).ifPresent(
                        newSpellData -> newSpellData.loadAll(oldSpellData.saveAll())
                )
        );

        event.getOriginal().invalidateCaps();
    }
}
