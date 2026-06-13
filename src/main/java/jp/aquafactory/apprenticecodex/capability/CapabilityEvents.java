package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AbsorptionAmplifyAmuletState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.BoundBowState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.BoundSwordState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowCastState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FocusStaffbowLoanState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MirageAvoidanceState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CapabilityEvents {
    private CapabilityEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        var registryAccess = event.getEntity().registryAccess();

        var oldPersonalInventory = Capabilities.getPersonalInventory(event.getOriginal()).orElse(null);
        var newPersonalInventory = Capabilities.getPersonalInventory(event.getEntity()).orElse(null);
        if (oldPersonalInventory != null && newPersonalInventory != null) {
            newPersonalInventory.deserializeNBT(registryAccess, oldPersonalInventory.serializeNBT(registryAccess));
        }

        var oldCompanionInventory = Capabilities.getCompanionTrunkInventory(event.getOriginal()).orElse(null);
        var newCompanionInventory = Capabilities.getCompanionTrunkInventory(event.getEntity()).orElse(null);
        if (oldCompanionInventory != null && newCompanionInventory != null) {
            if (event.isWasDeath()) {
                newCompanionInventory.copyNameOnlyFrom(oldCompanionInventory);
            } else {
                newCompanionInventory.copyAllFrom(registryAccess, oldCompanionInventory);
            }
        }

        var oldSpellData = Capabilities.getSpellData(event.getOriginal()).orElse(null);
        var newSpellData = Capabilities.getSpellData(event.getEntity()).orElse(null);
        if (oldSpellData != null && newSpellData != null) {
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

        var oldEnderGrimoire = Capabilities.getEnderGrimoireSpellbook(event.getOriginal()).orElse(null);
        var newEnderGrimoire = Capabilities.getEnderGrimoireSpellbook(event.getEntity()).orElse(null);
        if (oldEnderGrimoire != null && newEnderGrimoire != null) {
            newEnderGrimoire.load(oldEnderGrimoire.save());
        }
    }
}
