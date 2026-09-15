package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class PlunderLootGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PlunderLootGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void plunderChangesCountAndChanceWithoutChangingHands(GameTestHelper helper) {
        var player = player(helper);
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(hand, plunderBook(helper));
            // Better Combat等による論理手持ちの扱いも、実際のloot関数を通して確認する。
            var context = context(helper, player, false);
            assertLoot(helper, context, 4, true);
            assertUnchangedAcrossDeath(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unsupportedMainhandAndExistingLootingArePreserved(GameTestHelper helper) {
        var player = player(helper);
        var looting = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        for (int level : new int[]{0, 2, 5}) {
            var stack = new ItemStack(Items.STICK);
            if (level > 0) {
                stack.enchant(looting, level);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setItemInHand(InteractionHand.OFF_HAND, plunderBook(helper));
            assertLoot(helper, context(helper, player, false), 1 + Math.max(3, level), true);
            assertUnchangedAcrossDeath(helper, player);
        }
        if (ModList.get().isLoaded("malum")) {
            var staff = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", "unwinding_chaos"));
            helper.assertTrue(staff != Items.AIR, "Unwinding Chaos must be registered in the Malum profile");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(staff));
            assertLoot(helper, context(helper, player, false), 4, true);
            assertUnchangedAcrossDeath(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fireKillCreditAppliesPlunderWithoutMutation(GameTestHelper helper) {
        var player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND, plunderBook(helper));
        assertLoot(helper, context(helper, player, true), 4, true);
        assertUnchangedAcrossDeath(helper, player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void noPlunderAndOtherEnchantmentsKeepVanillaResults(GameTestHelper helper) {
        var player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        assertLoot(helper, context(helper, player, false), 1, false);
        // 非対応アイテムへの強制付与は略奪の効果を有効にしない。
        player.getMainHandItem().enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.PLUNDER), 3);
        assertLoot(helper, context(helper, player, false), 1, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, plunderBook(helper));
        var fortune = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        var function = new EnchantedCountIncreaseFunction.Builder(fortune, ConstantValue.exactly(1)).build();
        helper.assertTrue(function.apply(new ItemStack(Items.DIAMOND), context(helper, player, false)).getCount() == 1,
                "Plunder must not replace unrelated enchantment levels");
        helper.succeed();
    }

    private static Player player(GameTestHelper helper) {
        return ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "plunder_loot_test");
    }

    private static ItemStack plunderBook(GameTestHelper helper) {
        var book = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
        book.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.PLUNDER), 3);
        return book;
    }

    private static LootContext context(GameTestHelper helper, Player player, boolean fire) {
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        target.setLastHurtByPlayer(player);
        var source = fire ? target.damageSources().onFire() : target.damageSources().playerAttack(player);
        var params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, target)
                .withParameter(LootContextParams.ORIGIN, target.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .create(LootContextParamSets.ENTITY);
        return new LootContext.Builder(params).create(Optional.empty());
    }

    private static void assertLoot(GameTestHelper helper, LootContext context, int count, boolean chance) {
        var function = EnchantedCountIncreaseFunction.lootingMultiplier(
                helper.getLevel().registryAccess(), ConstantValue.exactly(1)).build();
        helper.assertTrue(function.apply(new ItemStack(Items.DIAMOND), context).getCount() == count,
                "Loot count should use the maximum of Looting and Plunder");
        var condition = LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(
                helper.getLevel().registryAccess(), 0, 1).build();
        helper.assertTrue(condition.test(context) == chance, "Loot chance should use Plunder without random ambiguity");
    }

    private static void assertUnchangedAcrossDeath(GameTestHelper helper, Player player) {
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        var mainBefore = main.copy();
        var offBefore = off.copy();
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        Consumer<LivingDeathEvent> cancel = event -> {
            if (event.getEntity() == target) {
                helper.assertTrue(ItemStack.matches(mainBefore, main) && ItemStack.matches(offBefore, off),
                        "Plunder must not mutate items even inside a death event");
                event.setCanceled(true);
                target.setHealth(1);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, cancel);
        try {
            target.hurt(target.damageSources().playerAttack(player), 1000);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(!target.isDeadOrDying(), "The death event must have been canceled");
        // ドロップイベントを発火しない独自死亡処理でも、復元を待たず手持ちが不変であることを確認する。
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(target, target.damageSources().playerAttack(player)));
        helper.assertTrue(player.getMainHandItem() == main && player.getOffhandItem() == off
                        && ItemStack.matches(mainBefore, main) && ItemStack.matches(offBefore, off),
                "Death without drops must preserve hand references and all existing item components");
    }
}
