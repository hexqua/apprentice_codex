package jp.aquafactory.apprenticecodex.gametest.support;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AbsorptionAmplifyAmuletState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MirageAvoidanceState;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class EquipmentStateTestSupport {
    private EquipmentStateTestSupport() {}

    public static ItemStack getEquippedAutocastAmulet(FakePlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof AutocastAmulet).stream()
                        .findFirst()
                        .map(SlotResult::stack)
                        .orElseThrow(() -> new IllegalStateException("Missing equipped Autocast Amulet for GameTest")))
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Autocast Amulet GameTest"));
    }

    public static ManaShieldCharmState getManaShieldCharmState(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            throw new IllegalStateException("Missing spell data for Mana Shield Charm GameTest");
        }
        return spellData.get(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE);
    }

    public static MirageAvoidanceState getMirageAvoidanceState(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            throw new IllegalStateException("Missing spell data for MirageAvoidance GameTest");
        }
        return spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE);
    }

    public static RemoteEyeState getRemoteEyeState(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            throw new IllegalStateException("Missing spell data for RemoteEye GameTest");
        }
        return spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE);
    }

    public static AbsorptionAmplifyAmuletState getAbsorptionAmplifyAmuletState(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            throw new IllegalStateException("Missing spell data for Absorption Amplify Amulet GameTest");
        }
        return spellData.get(CodexSpellStateTypeRegister.ABSORPTION_AMPLIFY_AMULET_STATE);
    }
}
