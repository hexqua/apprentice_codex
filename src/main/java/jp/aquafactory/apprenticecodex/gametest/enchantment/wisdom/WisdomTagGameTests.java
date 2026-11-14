package jp.aquafactory.apprenticecodex.gametest.enchantment.wisdom;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class WisdomTagGameTests {
    private WisdomTagGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void applicationAndActivationTagsAreIndependent(GameTestHelper helper) {
        var wisdom = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.WISDOM);
        for (var test : List.of(
                new ApplicationCase(Items.BLAZE_ROD, true, 24),
                new ApplicationCase(Items.GOLDEN_BOOTS, true, 20),
                new ApplicationCase(Items.BREEZE_ROD, false, 24),
                new ApplicationCase(Items.STICK, false, 20)
        )) {
            var stack = enchantedStack(helper, test.item(), 1);
            helper.assertFalse(stack.getItem() instanceof WisdomPolicy,
                    "Tag fixtures must not implement WisdomPolicy");
            helper.assertTrue(wisdom.value().canEnchant(stack) == test.applicable(),
                    test.item() + " direct application should follow only wisdom_enchantable");
            helper.assertTrue(stack.getItem().supportsEnchantment(stack, wisdom) == test.applicable(),
                    test.item() + " book application should follow only wisdom_enchantable");
            var player = player(helper);
            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
            assertExperience(helper, player, test.expectedExperience(), test.item() + " tag combination");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void activationTagsRespectEquipmentLocation(GameTestHelper helper) {
        // 実際に読み込まれたデータパックのタグを使い、別部位の許可が混ざらないことを検査する。
        for (var test : List.of(
                new LocationCase(Items.BREEZE_ROD, EquipmentSlot.MAINHAND, 24),
                new LocationCase(Items.BREEZE_ROD, EquipmentSlot.OFFHAND, 24),
                new LocationCase(Items.BREEZE_ROD, EquipmentSlot.HEAD, 20),
                new LocationCase(Items.BREEZE_ROD, null, 20),
                new LocationCase(Items.GOLDEN_HELMET, EquipmentSlot.HEAD, 21),
                new LocationCase(Items.GOLDEN_HELMET, EquipmentSlot.MAINHAND, 20),
                new LocationCase(Items.GOLDEN_HELMET, null, 20),
                new LocationCase(Items.GOLD_NUGGET, null, 21),
                new LocationCase(Items.GOLD_NUGGET, EquipmentSlot.MAINHAND, 20),
                new LocationCase(Items.GOLD_NUGGET, EquipmentSlot.HEAD, 20)
        )) {
            var player = player(helper);
            var stack = enchantedStack(helper, test.item(), 1);
            if (test.slot() == null) {
                CurioTestSupport.equipRingCurio(player, stack);
            } else {
                player.setItemSlot(test.slot(), stack);
            }
            assertExperience(helper, player, test.expectedExperience(), test.item() + " at " + test.slot());
        }
        var unenchantedPlayer = player(helper);
        unenchantedPlayer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BREEZE_ROD));
        assertExperience(helper, unenchantedPlayer, 20, "Tag membership without Wisdom");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void taggedEquipmentUsesHeldMaximumAndEquipmentSum(GameTestHelper helper) {
        var player = player(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, enchantedStack(helper, Items.BLAZE_ROD, 2));
        player.setItemSlot(EquipmentSlot.OFFHAND, enchantedStack(helper, Items.BREEZE_ROD, 3));
        player.setItemSlot(EquipmentSlot.HEAD, enchantedStack(helper, Items.GOLDEN_HELMET, 2));
        CurioTestSupport.equipRingCurio(player, enchantedStack(helper, Items.GOLD_NUGGET, 3));
        assertExperience(helper, player, 37, "Combined tagged equipment");
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        // 同 tick 内の Curios 検索 cache を使い回さず、各ケースを独立した装備状態で検査する。
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "wisdom_tag_test"));
    }

    private static ItemStack enchantedStack(GameTestHelper helper, Item item, int level) {
        var stack = new ItemStack(item);
        var wisdom = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.WISDOM);
        stack.enchant(wisdom, level);
        return stack;
    }

    private static void assertExperience(GameTestHelper helper, FakePlayer player, int expected, String context) {
        var victim = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()));
        var livingEvent = new LivingExperienceDropEvent(victim, player, 20);
        WisdomExperienceDropEvent.onLivingExperienceDrop(livingEvent);
        helper.assertTrue(livingEvent.getDroppedExperience() == expected,
                context + " mob XP expected=" + expected + " actual=" + livingEvent.getDroppedExperience());

        var blockEvent = new BlockDropsEvent(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                Blocks.DIAMOND_ORE.defaultBlockState(), null, new ArrayList<>(), player, player.getMainHandItem());
        blockEvent.setDroppedExperience(20);
        WisdomExperienceDropEvent.onBlockDrops(blockEvent);
        helper.assertTrue(blockEvent.getDroppedExperience() == expected,
                context + " mining XP expected=" + expected + " actual=" + blockEvent.getDroppedExperience());
    }

    private record ApplicationCase(Item item, boolean applicable, int expectedExperience) {
    }

    // null は Curios の ring スロットを表す。
    private record LocationCase(Item item, EquipmentSlot slot, int expectedExperience) {
    }
}
