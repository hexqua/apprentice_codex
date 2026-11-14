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
    public static void magiStaffInfusionRecipeSelectsOnlyCompleteIngredients(GameTestHelper helper) {
        if (MagiEngineersStaff.available()) {
            MagiEngineersStaffRecipeScenarios.recipeSelection(helper);
        } else {
            helper.assertTrue(helper.getLevel().getRecipeManager()
                            .byKey(ItemRegistry.MAGI_ENGINEERS_STAFF.getId()).isEmpty(),
                    "Missing Magitech must skip the infusion recipe");
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffKeepsEnchantmentPolicyWithoutMagitech(GameTestHelper helper) {
        var item = (MagiEngineersStaff) ItemRegistry.MAGI_ENGINEERS_STAFF.get();
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "magi_staff_test"));
        var stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.SHARPNESS))
                        && !item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.UNBREAKING))
                        && !item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.MENDING)),
                "Staff must accept sword enchantments and reject durability enchantments");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        if (!MagiEngineersStaff.available()) {
            for (var hand : InteractionHand.values()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.setItemInHand(hand, stack);
                item.use(helper.getLevel(), player, hand);
                item.inventoryTick(stack, helper.getLevel(), player, 0, true);
                helper.assertTrue(!player.isUsingItem(), "Missing Magitech must leave either hand inactive");
            }
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
    public static void magiStaffRespectsMainHandCasterPriority(GameTestHelper helper) {
        if (MagiEngineersStaff.available()) MagiEngineersStaffOffhandScenarios.priority(helper);
        else helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffSyncPreservesSourceAndPhase(GameTestHelper helper) {
        MagiEngineersStaffOffhandScenarios.packet(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffLootEnchantmentsRemainMainHandOnly(GameTestHelper helper) {
        MagiEngineersStaffOffhandScenarios.lootEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiStaffKeepsAmplifierBonusWithBetterCombat(GameTestHelper helper) {
        if (MagiEngineersStaff.available() && ModList.get().isLoaded("bettercombat")) {
            MagiEngineersStaffBetterCombatScenarios.amplifier(helper);
        } else helper.succeed();
    }
}
