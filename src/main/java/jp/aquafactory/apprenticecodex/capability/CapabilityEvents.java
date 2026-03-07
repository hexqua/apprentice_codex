package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellDataProvider;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookDataProvider;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventoryProvider;
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
            event.addCapability(EnderGrimoireSpellbookDataProvider.ID, new EnderGrimoireSpellbookDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();

        // 基本的にNBT経由で死亡後も継続させる.
        event.getOriginal().getCapability(Capabilities.PERSONAL_INVENTORY).ifPresent(
                oldInventory -> event.getEntity().getCapability(Capabilities.PERSONAL_INVENTORY).ifPresent(
                        newInventory -> newInventory.deserializeNBT(oldInventory.serializeNBT())
                )
        );
        event.getOriginal().getCapability(Capabilities.SPELL_DATA).ifPresent(
                oldSpellData -> event.getEntity().getCapability(Capabilities.SPELL_DATA).ifPresent(
                        newSpellData -> {
                            newSpellData.loadAll(oldSpellData.saveAll());
                            if (event.isWasDeath()) {
                                newSpellData.edit(CodexSpellStateTypeRegister.ABSORPTION_AMPLIFY_AMULET_STATE, state -> state.reset());
                            }
                        }
                )
        );
        event.getOriginal().getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(
                oldData -> event.getEntity().getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(
                        newData -> newData.load(oldData.save())
                )
        );

        event.getOriginal().invalidateCaps();
    }
}
