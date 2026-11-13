package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineersstaff;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MagiEngineersStaffGameTests {
    private MagiEngineersStaffGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffRejectsOffhandAndDurabilityEnchantments(GameTestHelper helper) {
        var item = (MagiEngineersStaff) ItemRegistry.MAGI_ENGINEERS_STAFF.get();
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_staff_test"));
        var stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult().consumesAction() == false,
                "Offhand use must not start a spell");
        helper.assertTrue(!player.isUsingItem(), "Offhand use must not change casting state");
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.SHARPNESS))
                        && !item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.UNBREAKING))
                        && !item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.MENDING)),
                "Staff must accept sword enchantments and reject durability enchantments");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        if (!MagiEngineersStaff.available()) {
            item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            item.inventoryTick(stack, helper.getLevel(), player, 0, true);
            helper.assertTrue(!player.isUsingItem(), "Missing Magitech must leave the registered staff inactive");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffConvertsOnlyWholePaymentsWhileHeld(GameTestHelper helper) {
        if (MagiEngineersStaff.available()) MagiEngineersStaffScenarios.conversion(helper);
        else helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffTransformsPowerWithoutChangingNativeSpells(GameTestHelper helper) {
        if (MagiEngineersStaff.available()) MagiEngineersStaffScenarios.power(helper);
        else helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffUsesSelectedSpellAndCancelsCharge(GameTestHelper helper) {
        if (MagiEngineersStaff.available()) MagiEngineersStaffScenarios.casting(helper);
        else helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffSeparatesEqualIdCasterInstances(GameTestHelper helper) {
        if (MagiEngineersStaff.available()) MagiEngineersStaffScenarios.equalIdCasters(helper);
        else helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffKeepsAmplifierBonusWithBetterCombat(GameTestHelper helper) {
        if (MagiEngineersStaff.available() && ModList.get().isLoaded("bettercombat")) {
            MagiEngineersStaffBetterCombatScenarios.amplifier(helper);
        } else helper.succeed();
    }
}
