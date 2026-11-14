package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineersstaff;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class MagiEngineersStaffBetterCombatScenarios {
    private MagiEngineersStaffBetterCombatScenarios() {
    }

    static void amplifier(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_better_test"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get()));
        player.getInventory().offhand.set(0, new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get()));
        var attributes = WeaponRegistry.getAttributes(player.getMainHandItem());
        helper.assertTrue(attributes != null && !attributes.isTwoHanded(), "Staff must resolve one-handed weapon data");
        try {
            helper.assertTrue(!BetterCombatOffhandAttributeRescueCompat.isRescueActive(player)
                            && player.getOffhandItem().is(ItemRegistry.IRON_SPELL_AMPLIFIER.get()),
                    "One-handed staff must expose the real offhand without attribute rescue");
            player.doTick();
            BetterCombatOffhandAttributeRescueCompat.sync(player);
            helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_POWER) > 1,
                    "Normally equipped offhand amplifier must increase the actual player spell power");
        } finally {
            BetterCombatOffhandAttributeRescueCompat.clear(player);
        }
        helper.succeed();
    }
}
