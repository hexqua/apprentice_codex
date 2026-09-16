package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.SummonedSkeleton;
import io.redspace.ironsspellbooks.entity.mobs.SummonedZombie;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

    @GameTest(template = TEMPLATE)
    public static void nonPlayerAttackerFallsBackWithoutLosingItsLooting(GameTestHelper helper) {
        var player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, plunderBook(helper));
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        var attacker = helper.spawn(EntityType.SKELETON, new BlockPos(0, 2, 2));
        // プレイヤーが削った敵を別のMobが攻撃しても、Minecraftの撃破帰属はプレイヤーに残る。
        target.hurt(target.damageSources().playerAttack(player), 1);
        target.invulnerableTime = 0;
        var source = target.damageSources().mobAttack(attacker);
        target.hurt(source, 1);
        helper.assertTrue(target.getKillCredit() == player, "Player kill credit must survive a subsequent mob hit");
        assertLoot(helper, context(helper, target, source), 4, true);

        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING), 5);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, sword);
        // Mob自体のLooting Vを、帰属先のPlunder IIIへ置き換えて減らさない。
        assertLoot(helper, context(helper, target, source), 6, true);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertLoot(helper, context(helper, target, source), 1, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, plunderBook(helper));
        var unrelated = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        assertLoot(helper, context(helper, unrelated, source), 1, false);

        var otherPlayer = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "plunder_other_attacker");
        assertLoot(helper, context(helper, target, target.damageSources().playerAttack(otherPlayer)), 1, false);
        // 所有者を失った召喚武器のように、攻撃者自体がLivingEntityでない場合も帰属を確認する。
        var ownerlessKnife = new PrecisionJackKnifeEntity(EntityRegistry.PRECISION_JACK_KNIFE.get(), helper.getLevel());
        var ownerlessSource = new DamageSource(source.typeHolder(), ownerlessKnife);
        assertLoot(helper, context(helper, target, ownerlessSource), 4, true);
        assertLoot(helper, context(helper, unrelated, ownerlessSource), 1, false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void raiseDeadMeleeAndArrowsUseTheirActualDamageAttribution(GameTestHelper helper) {
        var player = player(helper);
        var book = plunderBook(helper);
        var before = book.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        var level = helper.getLevel();
        var zombie = new SummonedZombie(level, false);
        var skeleton = new SummonedSkeleton(level, false);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        for (var summon : new Mob[]{zombie, skeleton}) {
            summon.setPos(helper.absoluteVec(new Vec3(0, 2, 2)));
            summon.setNoAi(true);
            level.addFreshEntity(summon);
            SummonManager.setOwner(summon, player);
        }
        try {
            // RaiseDeadが生成する2種の実際の近接処理は召喚者を攻撃者に設定する。
            for (var summon : new Mob[]{zombie, skeleton}) {
                var source = captureMeleeSource(helper, summon, target);
                helper.assertTrue(source.getEntity() == player && source.getDirectEntity() == summon,
                        "Raise Dead melee must attribute the damage to its summoner");
                assertLoot(helper, context(helper, target, source), 4, true);
            }

            skeleton.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            skeleton.performRangedAttack(target, 1);
            var arrows = level.getEntitiesOfClass(AbstractArrow.class, skeleton.getBoundingBox().inflate(8),
                    arrow -> arrow.getOwner() == skeleton);
            helper.assertTrue(arrows.size() == 1, "Raise Dead skeleton must fire an arrow owned by the skeleton");
            var arrow = arrows.getFirst();
            try {
                var source = target.damageSources().arrow(arrow, arrow.getOwner());
                // 所有関係だけで略奪を広げず、射撃はプレイヤー撃破帰属がある場合に補完する。
                assertLoot(helper, context(helper, target, source), 1, false);
                target.setLastHurtByPlayer(player);
                assertLoot(helper, context(helper, target, source), 4, true);
            } finally {
                arrow.discard();
            }
            helper.assertTrue(player.getMainHandItem() == book && ItemStack.matches(before, book),
                    "Summon loot calculations must preserve the summoner's item");
        } finally {
            zombie.discard();
            skeleton.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void summonedWeaponDamageKeepsPlayerAndNonPlayerAttribution(GameTestHelper helper) {
        var player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, plunderBook(helper));
        var mob = helper.spawn(EntityType.SKELETON, new BlockPos(0, 2, 2));
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        // SummonWeaponEntityの実装が作るDamageSourceを使い、ナイフだけを攻撃者と誤認しない。
        for (var owner : new LivingEntity[]{player, mob}) {
            var knife = new PrecisionJackKnifeEntity(EntityRegistry.PRECISION_JACK_KNIFE.get(), helper.getLevel(), owner) {
                DamageSource damageSourceForTest() {
                    return createCombatDamageSource(DamageTypes.PRECISION_JACK);
                }
            };
            var source = knife.damageSourceForTest();
            helper.assertTrue(source.getEntity() == owner && source.getDirectEntity() == knife,
                    "Summoned weapon must preserve both its owner and direct damage entity");
            if (owner == mob) {
                assertLoot(helper, context(helper, target, source), 1, false);
                target.setLastHurtByPlayer(player);
            }
            assertLoot(helper, context(helper, target, source), 4, true);
        }
        helper.succeed();
    }

    private static DamageSource captureMeleeSource(GameTestHelper helper, Mob attacker, LivingEntity target) {
        var captured = new AtomicReference<DamageSource>();
        Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getEntity() == target) {
                captured.set(event.getSource());
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingIncomingDamageEvent.class, listener);
        try {
            attacker.doHurtTarget(target);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.assertTrue(captured.get() != null, "Summon melee must reach the damage event");
        return captured.get();
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
        return context(helper, target, source);
    }

    private static LootContext context(GameTestHelper helper, LivingEntity target, DamageSource source) {
        var params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, target)
                .withParameter(LootContextParams.ORIGIN, target.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER,
                        target.getKillCredit() instanceof Player player ? player : null)
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
