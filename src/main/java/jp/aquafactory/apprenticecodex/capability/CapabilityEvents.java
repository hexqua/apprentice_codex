package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellDataProvider;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AbsorptionAmplifyAmuletState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.BoundBowState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.BoundSwordState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowCastState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowLoanState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MirageAvoidanceState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookDataProvider;
import jp.aquafactory.apprenticecodex.capability.companiontrunkinventory.CompanionTrunkInventoryProvider;
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
            event.addCapability(CompanionTrunkInventoryProvider.ID, new CompanionTrunkInventoryProvider());
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
        event.getOriginal().getCapability(Capabilities.COMPANION_TRUNK_INVENTORY).ifPresent(
                oldInventory -> event.getEntity().getCapability(Capabilities.COMPANION_TRUNK_INVENTORY).ifPresent(
                        newInventory -> {
                            if (event.isWasDeath()) {
                                newInventory.copyNameOnlyFrom(oldInventory);
                            } else {
                                newInventory.copyAllFrom(oldInventory);
                            }
                        }
                )
        );
        event.getOriginal().getCapability(Capabilities.SPELL_DATA).ifPresent(
                oldSpellData -> event.getEntity().getCapability(Capabilities.SPELL_DATA).ifPresent(
                        newSpellData -> {
                            newSpellData.loadAll(oldSpellData.saveAll());
                            if (event.isWasDeath()) {
                                newSpellData.edit(CodexSpellStateTypeRegister.ABSORPTION_AMPLIFY_AMULET_STATE, AbsorptionAmplifyAmuletState::reset);
                                newSpellData.edit(CodexSpellStateTypeRegister.BOUND_BOW_STATE, BoundBowState::reset);
                                newSpellData.edit(CodexSpellStateTypeRegister.BOUND_SWORD_STATE, BoundSwordState::reset);
                                newSpellData.edit(CodexSpellStateTypeRegister.COMPANION_TRUNK_STATE, state -> {
                                    state.active = false;
                                    state.maxHealth = 0.0;
                                    state.setTrunkUuid(null);
                                });
                                newSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, FocusStaffbowCastState::reset);
                                newSpellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_LOAN_STATE, FocusStaffbowLoanState::reset);
                                newSpellData.edit(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE, ManaShieldCharmState::reset);
                                newSpellData.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, MirageAvoidanceState::reset);
                                newSpellData.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, RemoteEyeState::reset);
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
