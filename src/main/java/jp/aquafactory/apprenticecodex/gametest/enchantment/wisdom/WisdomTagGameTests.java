package jp.aquafactory.apprenticecodex.gametest.enchantment.wisdom;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentApplicationGameTestSupport.createEnchantedBook;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class WisdomTagGameTests {
    private WisdomTagGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void applicationAndActivationTagsAreIndependent(GameTestHelper helper) {
        var wisdom = EnchantmentRegistry.WISDOM.get();
        for (var test : List.of(
                new ApplicationCase(Items.BLAZE_ROD, true, 24),
                new ApplicationCase(Items.GOLDEN_BOOTS, true, 20),
                new ApplicationCase(Items.IRON_NUGGET, false, 24),
                new ApplicationCase(Items.STICK, false, 20)
        )) {
            var stack = new ItemStack(test.item());
            helper.assertFalse(stack.getItem() instanceof WisdomPolicy,
                    "Tag fixtures must not implement WisdomPolicy");
            helper.assertTrue(EnchantmentHelper.getAvailableEnchantmentResults(30, stack, false).stream()
                            .anyMatch(instance -> instance.enchantment == wisdom) == test.applicable(),
                    test.item() + " enchanting table eligibility should follow wisdom_enchantable");
            var player = player(helper);
            var anvil = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
            anvil.getSlot(0).set(stack.copy());
            anvil.getSlot(1).set(createEnchantedBook(wisdom));
            helper.assertTrue((anvil.getSlot(2).getItem().getEnchantmentLevel(wisdom) == 1) == test.applicable(),
                    test.item() + " book application should follow wisdom_enchantable");
            stack.enchant(wisdom, 1);
            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
            assertExperience(helper, player, test.expectedExperience(), test.item() + " tag combination");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void activationTagsRespectEquipmentLocation(GameTestHelper helper) {
        // 実際に読み込まれたデータパックのタグを使い、別部位の許可が混ざらないことを検査する。
        for (var test : List.of(
                new LocationCase(Items.IRON_NUGGET, EquipmentSlot.MAINHAND, 24),
                new LocationCase(Items.IRON_NUGGET, EquipmentSlot.OFFHAND, 24),
                new LocationCase(Items.IRON_NUGGET, EquipmentSlot.HEAD, 20),
                new LocationCase(Items.IRON_NUGGET, null, 20),
                new LocationCase(Items.GOLDEN_HELMET, EquipmentSlot.HEAD, 21),
                new LocationCase(Items.GOLDEN_HELMET, EquipmentSlot.MAINHAND, 20),
                new LocationCase(Items.GOLDEN_HELMET, null, 20),
                new LocationCase(Items.GOLD_NUGGET, null, 21),
                new LocationCase(Items.GOLD_NUGGET, EquipmentSlot.MAINHAND, 20),
                new LocationCase(Items.GOLD_NUGGET, EquipmentSlot.HEAD, 20)
        )) {
            var player = player(helper);
            var stack = enchantedStack(test.item(), 1);
            if (test.slot() == null) {
                CurioTestSupport.equipRingCurio(player, stack);
            } else {
                player.setItemSlot(test.slot(), stack);
            }
            assertExperience(helper, player, test.expectedExperience(), test.item() + " at " + test.slot());
        }
        var unenchantedPlayer = player(helper);
        unenchantedPlayer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_NUGGET));
        assertExperience(helper, unenchantedPlayer, 20, "Tag membership without Wisdom");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void taggedEquipmentUsesHeldMaximumAndEquipmentSum(GameTestHelper helper) {
        var player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, enchantedStack(Items.BLAZE_ROD, 2));
        player.setItemSlot(EquipmentSlot.OFFHAND, enchantedStack(Items.IRON_NUGGET, 3));
        player.setItemSlot(EquipmentSlot.HEAD, enchantedStack(Items.GOLDEN_HELMET, 2));
        CurioTestSupport.equipRingCurio(player, enchantedStack(Items.GOLD_NUGGET, 3));
        assertExperience(helper, player, 21, 39, "Combined tagged equipment with rounding");

        var event = new BlockEvent.BreakEvent(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                Blocks.SCULK.defaultBlockState(), player);
        event.setExpToDrop(1);
        MinecraftForge.EVENT_BUS.post(event);
        helper.assertTrue(event.getExpToDrop() == 2, "The Forge break event must increase sculk's fixed experience");
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        // 同 tick 内の Curios 検索 cache を使い回さず、各ケースを独立した装備状態で検査する。
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "wisdom_tag_test"));
    }

    private static ItemStack enchantedStack(Item item, int level) {
        var stack = new ItemStack(item);
        stack.enchant(EnchantmentRegistry.WISDOM.get(), level);
        return stack;
    }

    private static void assertExperience(GameTestHelper helper, FakePlayer player, int expected, String context) {
        assertExperience(helper, player, 20, expected, context);
    }

    private static void assertExperience(GameTestHelper helper, FakePlayer player, int initial, int expected, String context) {
        var victim = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
        var livingEvent = new LivingExperienceDropEvent(victim, player, initial);
        MinecraftForge.EVENT_BUS.post(livingEvent);
        helper.assertTrue(livingEvent.getDroppedExperience() == expected,
                context + " mob XP expected=" + expected + " actual=" + livingEvent.getDroppedExperience());

        var blockEvent = new BlockEvent.BreakEvent(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                Blocks.DIAMOND_ORE.defaultBlockState(), player);
        blockEvent.setExpToDrop(initial);
        // Curios がイベント上で鉱石の経験値を再抽選するため、固定値での計算は叡智のハンドラで検証する。
        WisdomExperienceDropEvent.onBlockBreak(blockEvent);
        helper.assertTrue(blockEvent.getExpToDrop() == expected,
                context + " mining XP expected=" + expected + " actual=" + blockEvent.getExpToDrop());
    }

    private record ApplicationCase(Item item, boolean applicable, int expectedExperience) {
    }

    // null は Curios の ring スロットを表す。
    private record LocationCase(Item item, EquipmentSlot slot, int expectedExperience) {
    }
}
