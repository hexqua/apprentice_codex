package jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellchargedGreatswordServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.addExpectedMalumSpiritPlunderIfPresent;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertExactEnchantmentSurfaces;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.collectAllowedEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_SOUL_SHATTER_CAPABLE_WEAPON;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertModifierWithId;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertSingleModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.toModifierMultimap;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.postSpellOnCast;
import static jp.aquafactory.apprenticecodex.gametest.support.UpgradeTestSupport.assertMainhandUpgradeBridge;
import static jp.aquafactory.apprenticecodex.gametest.support.UpgradeTestSupport.assertUpgradeable;

final class SpellchargedGreatswordGameTestScenarios {
    private SpellchargedGreatswordGameTestScenarios() {}

    public static void spellchargedGreatswordKeepsExpectedStatsTagsAndEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var stack = new ItemStack(item);
            helper.assertTrue(stack.getMaxDamage() == SpellchargedGreatsword.DURABILITY,
                    "Spellcharged Greatsword durability should be " + SpellchargedGreatsword.DURABILITY
                            + " but got " + stack.getMaxDamage());
            helper.assertTrue(item.getEnchantmentValue(stack) == SpellchargedGreatsword.ENCHANTMENT_VALUE,
                    "Spellcharged Greatsword enchantability should be " + SpellchargedGreatsword.ENCHANTMENT_VALUE
                            + " but got " + item.getEnchantmentValue(stack));

            var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    SpellchargedGreatsword.DISPLAY_ATTACK_DAMAGE - 1.0D,
                    "Spellcharged Greatsword attack damage modifier should display as 8 damage"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADD_VALUE,
                    SpellchargedGreatsword.DISPLAY_ATTACK_SPEED - 4.0D,
                    "Spellcharged Greatsword attack speed modifier should display as 1.1 speed"
            );
            assertSpellchargedGreatswordEntityReach(
                    helper,
                    item,
                    stack,
                    ApprenticeCodexServerConfig.spellchargedGreatswordConfig().normalEntityReachBonus(),
                    "Spellcharged Greatsword entity reach modifier should match the normal server config"
            );
            assertUpgradeable(helper, stack, "Spellcharged Greatsword should remain upgradeable");
            assertMainhandUpgradeBridge(helper, stack, "Spellcharged Greatsword should keep mainhand upgrade modifiers");

            helper.assertTrue(stack.is(MALUM_SOUL_SHATTER_CAPABLE_WEAPON),
                    "Spellcharged Greatsword is missing malum:soul_shatter_capable_weapon");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedSpellchargedGreatswordEnchantments(helper.getLevel().registryAccess(), stack),
                    "Spellcharged Greatsword"
            );
        });
    }

    public static void spellchargedGreatswordChargeMathDecayAndAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var stack = new ItemStack(item);
            var gameTime = helper.getLevel().getGameTime();

            helper.assertTrue(SpellchargedGreatsword.computeChargeGainTicks(20, 20) == 20.0D,
                    "Spellcharged Greatsword should halve charge gain at 40 ticks or less");
            helper.assertTrue(SpellchargedGreatsword.computeChargeGainTicks(80, 200) == 200.0D,
                    "Spellcharged Greatsword charge gain should be capped to 200 ticks");

            SpellchargedGreatsword.addCharge(stack, gameTime, 200.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime, 200.0D, 1,
                    "Spellcharged Greatsword should reach level 1 at 200 charge ticks");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 2.0D, -0.0D,
                    "Spellcharged Greatsword level 1 attributes");

            SpellchargedGreatsword.addCharge(stack, gameTime, 200.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime, 400.0D, 2,
                    "Spellcharged Greatsword should reach level 2 at 400 charge ticks");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 4.0D, -0.2D,
                    "Spellcharged Greatsword level 2 attributes");

            SpellchargedGreatsword.addCharge(stack, gameTime, 400.0D);
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime + 100L, 800.0D, 3,
                    "Spellcharged Greatsword should keep full charge during the 5 second decay delay");
            assertSpellchargedGreatswordAttackAttributes(helper, item, stack, 8.0D, -0.5D,
                    "Spellcharged Greatsword level 3 attributes");

            helper.assertTrue(
                    Math.abs(SpellchargedGreatsword.getEffectiveChargeTicks(stack, gameTime + 101L) - 796.0D) < 1.0e-9D,
                    "Spellcharged Greatsword should decay 4 charge ticks per tick after the delay");
            helper.assertFalse(SpellchargedGreatsword.refreshDecay(stack, gameTime + 299L),
                    "Spellcharged Greatsword should not reset before charge reaches zero");
            helper.assertTrue(SpellchargedGreatsword.getChargeLevel(stack, gameTime + 299L) == 3,
                    "Spellcharged Greatsword decay should not lower level before charge reaches zero");
            helper.assertTrue(SpellchargedGreatsword.refreshDecay(stack, gameTime + 300L),
                    "Spellcharged Greatsword should reset when decay reaches zero");
            assertSpellchargedGreatswordChargeState(helper, stack, gameTime + 300L, 0.0D, 0,
                    "Spellcharged Greatsword should clear charge and level after full decay");

            var staleLevelStack = new ItemStack(item);
            SpellchargedGreatsword.addCharge(staleLevelStack, gameTime, 800.0D);
            SpellchargedGreatsword.addCharge(staleLevelStack, gameTime + 300L, 20.0D);
            assertSpellchargedGreatswordChargeState(helper, staleLevelStack, gameTime + 300L, 20.0D, 0,
                    "Spellcharged Greatsword should not restore a stale level after charge fully decays");

            var inventoryTickPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_inventory_decay_test");
            var inventoryTickStack = new ItemStack(item);
            var fullyDecayedChargeTime = helper.getLevel().getGameTime()
                    - SpellchargedGreatsword.DECAY_DELAY_TICKS
                    - (long) (SpellchargedGreatsword.MAX_CHARGE_TICKS / SpellchargedGreatsword.DECAY_TICKS_PER_TICK);
            SpellchargedGreatsword.addCharge(inventoryTickStack, fullyDecayedChargeTime, 800.0D);
            inventoryTickPlayer.getInventory().setItem(9, inventoryTickStack);
            item.inventoryTick(inventoryTickStack, helper.getLevel(), inventoryTickPlayer, 9, false);
            assertSpellchargedGreatswordChargeState(helper, inventoryTickStack, helper.getLevel().getGameTime(), 0.0D, 0,
                    "Spellcharged Greatsword should clear stale charge while ticking outside the main hand");

            var staleAttributeStack = new ItemStack(item);
            SpellchargedGreatsword.addCharge(staleAttributeStack, fullyDecayedChargeTime, 800.0D);
            assertSpellchargedGreatswordAttackAttributes(helper, item, staleAttributeStack, 0.0D, 0.0D,
                    "Spellcharged Greatsword stale charge should not apply attack attributes before inventory sync");
        });
    }

    public static void spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var level = helper.getLevel();

            var levelOneStack = new ItemStack(item);
            var levelOnePlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_level_one_overcharge_test");
            SpellchargedGreatsword.addCharge(levelOneStack, level.getGameTime(), 200.0D);
            item.releaseUsing(
                    levelOneStack,
                    level,
                    levelOnePlayer,
                    item.getUseDuration(levelOneStack, levelOnePlayer) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(levelOneStack),
                    "Spellcharged Greatsword level 1 should not enter overcharge mode");

            var levelTwoStack = new ItemStack(item);
            var levelTwoPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_level_two_overcharge_test");
            SpellchargedGreatsword.addCharge(levelTwoStack, level.getGameTime(), 400.0D);
            item.releaseUsing(
                    levelTwoStack,
                    level,
                    levelTwoPlayer,
                    item.getUseDuration(levelTwoStack, levelTwoPlayer) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(levelTwoStack),
                    "Spellcharged Greatsword level 2 should enter overcharge mode");
            assertSpellchargedGreatswordChargeState(helper, levelTwoStack, level.getGameTime(), 0.0D, 0,
                    "Spellcharged Greatsword overcharge should consume stored charge");
            assertSpellchargedGreatswordAttackAttributes(helper, item, levelTwoStack, 0.0D, 0.2D,
                    "Spellcharged Greatsword overcharge configured attributes");
            assertSpellchargedGreatswordEntityReach(
                    helper,
                    item,
                    levelTwoStack,
                    ApprenticeCodexServerConfig.spellchargedGreatswordConfig().overchargeEntityReachBonus(),
                    "Spellcharged Greatsword overcharge entity reach should match the server config"
            );

            helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(
                            levelTwoStack,
                            level.getGameTime() + SpellchargedGreatsword.OVERCHARGE_LEVEL_2_DURATION_TICKS
                    ),
                    "Spellcharged Greatsword level 2 overcharge should end after 10 seconds");
            var expiredOverchargeStack = levelTwoStack.copy();
            CustomData.update(DataComponents.CUSTOM_DATA, expiredOverchargeStack, tag ->
                    tag.putLong("SpellchargedGreatswordOverchargeEndGameTime", level.getGameTime() - 1L));
            helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(expiredOverchargeStack),
                    "Spellcharged Greatsword should not treat expired overcharge NBT as active");
            item.inventoryTick(expiredOverchargeStack, level, levelTwoPlayer, 0, false);
            assertSpellchargedGreatswordAttackAttributes(helper, item, expiredOverchargeStack, 0.0D, 0.0D,
                    "Spellcharged Greatsword expired overcharge should not use overcharge attributes");

            var levelThreeStack = new ItemStack(item);
            var levelThreePlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_level_three_overcharge_test");
            SpellchargedGreatsword.addCharge(levelThreeStack, level.getGameTime(), 800.0D);
            item.releaseUsing(
                    levelThreeStack,
                    level,
                    levelThreePlayer,
                    item.getUseDuration(levelThreeStack, levelThreePlayer) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(
                            levelThreeStack,
                            level.getGameTime() + SpellchargedGreatsword.OVERCHARGE_LEVEL_2_DURATION_TICKS
                    ),
                    "Spellcharged Greatsword level 3 overcharge should last longer than level 2");
        });
    }

    public static void spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(GameTestHelper helper) {
        var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "spellcharged_greatsword_overcharge_release_test");

        var staleLevelStack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, staleLevelStack);
        SpellchargedGreatsword.addCharge(staleLevelStack,
                level.getGameTime() - SpellchargedGreatsword.DECAY_DELAY_TICKS - 100L, 400.0D);
        var staleResult = item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(staleResult.getResult() == InteractionResult.PASS,
                "Spellcharged Greatsword should not start overcharge activation after charge fully decays");

        var stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        SpellchargedGreatsword.addCharge(stack, level.getGameTime() - SpellchargedGreatsword.DECAY_DELAY_TICKS - 20L,
                500.0D);
        var decayedCharge = SpellchargedGreatsword.getEffectiveChargeTicks(stack, level.getGameTime());

        var result = item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == InteractionResult.CONSUME,
                "Spellcharged Greatsword overcharge activation use should consume interaction");
        item.onUseTick(
                level,
                player,
                stack,
                item.getUseDuration(stack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
        );
        helper.assertTrue(Math.abs(
                SpellchargedGreatsword.getEffectiveChargeTicks(stack,
                        level.getGameTime() + SpellchargedGreatsword.DECAY_DELAY_TICKS) - decayedCharge) < 1.0e-9D,
                "Spellcharged Greatsword overcharge activation hold should pause normal charge decay"
        );
        helper.assertFalse(SpellchargedGreatsword.isOverchargeActive(stack),
                "Spellcharged Greatsword should not enter overcharge before release");

        item.releaseUsing(
                stack,
                level,
                player,
                item.getUseDuration(stack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
        );
        helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(stack),
                "Spellcharged Greatsword should enter overcharge when activation hold is released");

        var overchargedUse = item.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(overchargedUse.getResult() == InteractionResult.PASS,
                "Spellcharged Greatsword overcharge mode should not provide a right-click function");
        helper.succeed();
    }

    public static void spellchargedGreatswordClampsPersistedFutureOvercharge(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gameTime = helper.getLevel().getGameTime();
            var stack = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putDouble("SpellchargedGreatswordChargeTicks", 400.0D);
                tag.putLong("SpellchargedGreatswordLastChargeGameTime", gameTime + 72000L);
                tag.putInt("SpellchargedGreatswordChargeLevel", 2);
                tag.putInt("SpellchargedGreatswordOverchargeRemainingTicks", 72000);
                tag.putInt("SpellchargedGreatswordOverchargeMaxTicks", SpellchargedGreatsword.OVERCHARGE_LEVEL_3_DURATION_TICKS);
                tag.putLong("SpellchargedGreatswordOverchargeActivatedGameTime", gameTime + 72000L);
                tag.putLong("SpellchargedGreatswordOverchargeEndGameTime", gameTime + 72000L);
                tag.putLong("SpellchargedGreatswordOverchargeFadeStartGameTime", gameTime + 72000L);
            });

            helper.assertTrue(SpellchargedGreatsword.sanitizePersistentGameTimes(stack, gameTime),
                    "Spellcharged Greatsword sanitizer should rewrite future persisted game times");
            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            helper.assertTrue(tag.getLong("SpellchargedGreatswordLastChargeGameTime") <= gameTime,
                    "Spellcharged Greatsword future last charge time should be clamped to now");
            helper.assertTrue(tag.getLong("SpellchargedGreatswordOverchargeActivatedGameTime") <= gameTime,
                    "Spellcharged Greatsword future overcharge activation time should be clamped to now");
            helper.assertTrue(tag.getLong("SpellchargedGreatswordOverchargeEndGameTime")
                            <= gameTime + SpellchargedGreatsword.OVERCHARGE_LEVEL_3_DURATION_TICKS,
                    "Spellcharged Greatsword future overcharge end time should be clamped to max overcharge duration");
            helper.assertTrue(tag.getLong("SpellchargedGreatswordOverchargeFadeStartGameTime") <= gameTime,
                    "Spellcharged Greatsword future fade start time should be clamped to now");
        });
    }

    public static void spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_sweeping_bonus_test");
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));

            var stack = new ItemStack(item);
            var sweepingEdge = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE);
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, stack) == 1,
                    "Spellcharged Greatsword should act as Sweeping Edge I without the enchantment");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, stack, 0.5D,
                    "Spellcharged Greatsword should apply Sweeping Edge I ratio without the enchantment");
            assertAabbClose(helper, item.getSweepHitBox(stack, player, target),
                    target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D),
                    "Spellcharged Greatsword normal sweep hitbox");

            var enchantedStack = new ItemStack(item);
            var enchantedStackEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantedStackEnchantments.set(sweepingEdge, 3);
            EnchantmentHelper.setEnchantments(enchantedStack, enchantedStackEnchantments.toImmutable());
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, enchantedStack) == 4,
                    "Spellcharged Greatsword should add one virtual Sweeping Edge level normally");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, enchantedStack, 0.8D,
                    "Spellcharged Greatsword should apply actual plus virtual Sweeping Edge ratio normally");

            var overchargedStack = new ItemStack(item);
            SpellchargedGreatsword.addCharge(overchargedStack, level.getGameTime(), 400.0D);
            item.releaseUsing(
                    overchargedStack,
                    level,
                    player,
                    item.getUseDuration(overchargedStack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(overchargedStack),
                    "Spellcharged Greatsword test stack should enter overcharge mode");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, overchargedStack) == 3,
                    "Overcharged Spellcharged Greatsword should act as Sweeping Edge III without the enchantment");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, overchargedStack, 0.75D,
                    "Overcharged Spellcharged Greatsword should apply Sweeping Edge III ratio without the enchantment");
            assertAabbClose(helper, item.getSweepHitBox(overchargedStack, player, target),
                    target.getBoundingBox().inflate(3.0D, 0.25D, 3.0D),
                    "Spellcharged Greatsword overcharge sweep hitbox");
            CustomData.update(DataComponents.CUSTOM_DATA, overchargedStack, tag ->
                    tag.putLong("SpellchargedGreatswordOverchargeEndGameTime", level.getGameTime() - 1L));
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, overchargedStack) == 1,
                    "Expired overcharge Spellcharged Greatsword should use the normal Sweeping Edge bonus");
            assertSpellchargedGreatswordSweepingDamageRatio(helper, overchargedStack, 0.5D,
                    "Expired overcharge Spellcharged Greatsword should apply normal Sweeping Edge ratio");
            assertAabbClose(helper, item.getSweepHitBox(overchargedStack, player, target),
                    target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D),
                    "Expired overcharge Spellcharged Greatsword should use the normal sweep hitbox");

            var enchantedOverchargedStack = new ItemStack(item);
            var enchantedOverchargedStackEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantedOverchargedStackEnchantments.set(sweepingEdge, 3);
            EnchantmentHelper.setEnchantments(enchantedOverchargedStack, enchantedOverchargedStackEnchantments.toImmutable());
            SpellchargedGreatsword.addCharge(enchantedOverchargedStack, level.getGameTime(), 400.0D);
            item.releaseUsing(
                    enchantedOverchargedStack,
                    level,
                    player,
                    item.getUseDuration(enchantedOverchargedStack, player)
                            - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
            );
            helper.assertTrue(
                    EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, enchantedOverchargedStack) == 6,
                    "Overcharged Spellcharged Greatsword should add three virtual Sweeping Edge levels"
            );
            assertSpellchargedGreatswordSweepingDamageRatio(helper, enchantedOverchargedStack, 6.0D / 7.0D,
                    "Overcharged Spellcharged Greatsword should apply actual plus virtual Sweeping Edge ratio");
        });
    }

    public static void spellchargedGreatswordServerConfigOverridesCombatStats(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var config = new SpellchargedGreatswordServerConfig.Values(
                    11.0D,
                    -0.11D,
                    12.0D,
                    -0.22D,
                    13.0D,
                    -0.33D,
                    21.0D,
                    0.44D,
                    0.75D,
                    1.5D,
                    2,
                    5
            );

            try (var ignored = ApprenticeCodexServerConfig.useSpellchargedGreatswordConfigOverrideForGameTest(config)) {
                var sweepingEdge = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE);
                var gameTime = helper.getLevel().getGameTime();
                var levelOneStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(levelOneStack, gameTime, 200.0D);
                assertSpellchargedGreatswordAttackAttributes(helper, item, levelOneStack, 11.0D, -0.11D,
                        "Spellcharged Greatsword level 1 configured attributes");
                assertSpellchargedGreatswordEntityReach(helper, item, levelOneStack, 0.75D,
                        "Spellcharged Greatsword normal configured entity reach");
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, levelOneStack) == 2,
                        "Spellcharged Greatsword normal Sweeping Edge bonus should use server config");

                var levelTwoStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(levelTwoStack, gameTime, 400.0D);
                assertSpellchargedGreatswordAttackAttributes(helper, item, levelTwoStack, 12.0D, -0.22D,
                        "Spellcharged Greatsword level 2 configured attributes");

                var levelThreeStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(levelThreeStack, gameTime, 800.0D);
                assertSpellchargedGreatswordAttackAttributes(helper, item, levelThreeStack, 13.0D, -0.33D,
                        "Spellcharged Greatsword level 3 configured attributes");

                var overchargedStack = new ItemStack(item);
                var overchargedPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "spellcharged_greatsword_config_overcharge_test");
                SpellchargedGreatsword.addCharge(overchargedStack, helper.getLevel().getGameTime(), 400.0D);
                item.releaseUsing(
                        overchargedStack,
                        helper.getLevel(),
                        overchargedPlayer,
                        item.getUseDuration(overchargedStack, overchargedPlayer)
                                - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
                );
                assertSpellchargedGreatswordAttackAttributes(helper, item, overchargedStack, 21.0D, 0.44D,
                        "Spellcharged Greatsword overcharge configured attributes");
                assertSpellchargedGreatswordEntityReach(helper, item, overchargedStack, 1.5D,
                        "Spellcharged Greatsword overcharge configured entity reach");
                helper.assertTrue(
                        EnchantmentHelper.getItemEnchantmentLevel(sweepingEdge, overchargedStack) == 5,
                        "Spellcharged Greatsword overcharge Sweeping Edge bonus should use server config"
                );
            }
        });
    }

    public static void spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            try {
                var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "spellcharged_greatsword_bettercombat_attributes_test");
                var normalStack = new ItemStack(item);
                var overchargedStack = new ItemStack(item);
                SpellchargedGreatsword.addCharge(overchargedStack, helper.getLevel().getGameTime(), 400.0D);
                item.releaseUsing(
                        overchargedStack,
                        helper.getLevel(),
                        player,
                        item.getUseDuration(overchargedStack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
                );

                var weaponRegistry = Class.forName("net.bettercombat.logic.WeaponRegistry");
                var getAttributes = weaponRegistry.getMethod("getAttributes", ItemStack.class);
                var normalAttributes = getAttributes.invoke(null, normalStack);
                var chargedAttributes = getAttributes.invoke(null, overchargedStack);

                helper.assertTrue(readBetterCombatRangeBonus(normalAttributes) == 0.75D,
                        "Spellcharged Greatsword normal Better Combat attack range should come from the base file");
                helper.assertTrue(readBetterCombatRangeBonus(chargedAttributes) == 1.25D,
                        "Spellcharged Greatsword overcharge Better Combat attack range should come from the charged file");
                helper.assertTrue(readBetterCombatAttackCount(chargedAttributes) == 2,
                        "Spellcharged Greatsword overcharge Better Combat attacks should come from the charged file");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to inspect Better Combat weapon attributes", exception);
            }
        });
    }

    public static void spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(EpicFightCompat.MOD_ID)) {
                return;
            }

            try {
                var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "spellcharged_greatsword_epicfight_innate_test");
                var stack = new ItemStack(item);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);

                helper.assertTrue(hasExpectedSpellchargedGreatswordEpicFightSkills(player, stack),
                        "Spellcharged Greatsword Epic Fight skills should be Sweeping Edge and Swordmaster");
                assertSpellchargedGreatswordEpicFightOverchargedCollider(
                        helper,
                        item,
                        "Spellcharged Greatsword overcharge Epic Fight collider"
                );
                helper.assertTrue(setEpicFightSweepingEdgeCharge(player, 0),
                        "Spellcharged Greatsword Epic Fight test could not resolve weapon innate skill container");

                SpellchargedGreatsword.addCharge(stack, helper.getLevel().getGameTime(), 400.0D);
                item.releaseUsing(
                        stack,
                        helper.getLevel(),
                        player,
                        item.getUseDuration(stack, player) - SpellchargedGreatsword.OVERCHARGE_ACTIVATION_HOLD_TICKS
                );
                helper.assertTrue(SpellchargedGreatsword.isOverchargeActive(stack),
                        "Spellcharged Greatsword Epic Fight test stack should enter overcharge mode");

                invokeSpellchargedGreatswordEpicFightTick(player);

                var expectedStack = getEpicFightSweepingEdgeMaxStack();
                helper.assertTrue(getEpicFightSweepingEdgeCharge(player) == expectedStack,
                        "Spellcharged Greatsword overcharge should refill Epic Fight Sweeping Edge charge to "
                                + expectedStack);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to inspect Spellcharged Greatsword Epic Fight capability",
                        exception);
            }
        });
    }

    public static void spellchargedGreatswordChargeEventRequiresMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.ARCANE_BLAST.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "spellcharged_greatsword_mainhand_charge_test");
            var greatsword = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, greatsword);

            postSpellOnCast(player, spell, 1);
            var chargedMainhand = player.getMainHandItem();
            helper.assertTrue(SpellchargedGreatsword.getEffectiveChargeTicks(
                            chargedMainhand,
                            helper.getLevel().getGameTime()) > 0.0D,
                    "Spellcharged Greatsword should charge from spell casts while held in mainhand");

            var offhandOnly = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, offhandOnly);
            postSpellOnCast(player, spell, 1);
            helper.assertTrue(SpellchargedGreatsword.getEffectiveChargeTicks(
                            player.getOffhandItem(),
                            helper.getLevel().getGameTime()) == 0.0D,
                    "Spellcharged Greatsword should not charge from offhand-only casts");
        });
    }

    public static void spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        var spell = SpellRegistry.FORCE_FIELD.get();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "spellcharged_greatsword_continuous_refresh_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Spellcharged Greatsword continuous test could not resolve magic data");
        magicData.setSyncedData(new SyncedSpellData(player));

        var greatsword = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
        SpellchargedGreatsword.addCharge(greatsword, helper.getLevel().getGameTime(), 400.0D);
        player.setItemInHand(InteractionHand.MAIN_HAND, greatsword);
        magicData.initiateCast(spell, 1, 200, CastSource.SPELLBOOK, "gametest");
        postSpellOnCast(player, spell, 1);

        var firstChargeTime = helper.getLevel().getGameTime();
        var chargeAfterFirstCast = SpellchargedGreatsword.getEffectiveChargeTicks(
                player.getMainHandItem(),
                firstChargeTime
        );
        helper.assertTrue(chargeAfterFirstCast >= 400.0D,
                "Spellcharged Greatsword should keep prepared charge after the first CONTINUOUS cast event");
        var levelAfterFirstCast = SpellchargedGreatsword.getChargeLevel(player.getMainHandItem(), firstChargeTime);

        helper.runAtTickTime(SpellchargedGreatsword.DECAY_DELAY_TICKS + 10, () -> {
            var beforeRefreshTime = helper.getLevel().getGameTime();
            var chargeBeforeRefresh = SpellchargedGreatsword.getEffectiveChargeTicks(
                    player.getMainHandItem(),
                    beforeRefreshTime
            );
            helper.assertTrue(chargeBeforeRefresh < chargeAfterFirstCast,
                    "Spellcharged Greatsword test should reach the old decay window before the duplicate event");

            postSpellOnCast(player, spell, 1);
            assertSpellchargedGreatswordChargeState(
                    helper,
                    player.getMainHandItem(),
                    beforeRefreshTime,
                    chargeBeforeRefresh,
                    levelAfterFirstCast,
                    "Spellcharged Greatsword duplicate CONTINUOUS event should snapshot decayed charge"
            );
            helper.succeed();
        });
    }

    public static void spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        var spell = SpellRegistry.ARCHER_MULTIPLE.get();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "spellcharged_greatsword_recast_refresh_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Spellcharged Greatsword recast test could not resolve magic data");

        var greatsword = new ItemStack(ItemRegistry.SPELLCHARGED_GREATSWORD.get());
        SpellchargedGreatsword.addCharge(greatsword, helper.getLevel().getGameTime(), 400.0D);
        player.setItemInHand(InteractionHand.MAIN_HAND, greatsword);
        magicData.getPlayerRecasts().addRecast(new RecastInstance(
                spell.getSpellId(),
                1,
                2,
                1000,
                CastSource.SPELLBOOK,
                null
        ), magicData);

        helper.runAtTickTime(SpellchargedGreatsword.DECAY_DELAY_TICKS + 10, () -> {
            var beforeRefreshTime = helper.getLevel().getGameTime();
            var chargeBeforeRefresh = SpellchargedGreatsword.getEffectiveChargeTicks(
                    player.getMainHandItem(),
                    beforeRefreshTime
            );
            helper.assertTrue(chargeBeforeRefresh < 400.0D,
                    "Spellcharged Greatsword test should reach the old decay window before the Recast event");

            postSpellOnCast(player, spell, 1);
            assertSpellchargedGreatswordChargeState(
                    helper,
                    player.getMainHandItem(),
                    beforeRefreshTime,
                    chargeBeforeRefresh,
                    2,
                    "Spellcharged Greatsword Recast event should snapshot decayed charge"
            );
            helper.succeed();
        });
    }

    private static void assertSpellchargedGreatswordChargeState(
            GameTestHelper helper,
            ItemStack stack,
            long gameTime,
            double expectedChargeTicks,
            int expectedChargeLevel,
            String message
    ) {
        var actualChargeTicks = SpellchargedGreatsword.getEffectiveChargeTicks(stack, gameTime);
        helper.assertTrue(Math.abs(actualChargeTicks - expectedChargeTicks) < 1.0e-9D,
                message + ": expected charge " + expectedChargeTicks + " but got " + actualChargeTicks);
        helper.assertTrue(SpellchargedGreatsword.getChargeLevel(stack, gameTime) == expectedChargeLevel,
                message + ": expected level " + expectedChargeLevel + " but got "
                        + SpellchargedGreatsword.getChargeLevel(stack, gameTime));
    }

    private static void assertSpellchargedGreatswordAttackAttributes(
            GameTestHelper helper,
            SpellchargedGreatsword item,
            ItemStack stack,
            double expectedDamageBonus,
            double expectedSpeedBonus,
            String message
    ) {
        assertSpellchargedGreatswordAttackAttributeModifiers(
                helper,
                item.getDefaultAttributeModifiers(stack),
                expectedDamageBonus,
                expectedSpeedBonus,
                message + " item defaults"
        );
        assertSpellchargedGreatswordAttackAttributeModifiers(
                helper,
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY),
                expectedDamageBonus,
                expectedSpeedBonus,
                message + " component"
        );
        assertSpellchargedGreatswordAttackAttributeModifiers(
                helper,
                stack.getAttributeModifiers(),
                expectedDamageBonus,
                expectedSpeedBonus,
                message + " stack tooltip"
        );
    }

    private static void assertSpellchargedGreatswordAttackAttributeModifiers(
            GameTestHelper helper,
            ItemAttributeModifiers attributeModifiers,
            double expectedDamageBonus,
            double expectedSpeedBonus,
            String message
    ) {
        var modifiers = toModifierMultimap(attributeModifiers);
        assertModifierWithId(
                helper,
                modifiers.get(Attributes.ATTACK_DAMAGE),
                VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                SpellchargedGreatsword.DISPLAY_ATTACK_DAMAGE - 1.0D + expectedDamageBonus,
                message + " attack damage"
        );
        assertModifierWithId(
                helper,
                modifiers.get(Attributes.ATTACK_SPEED),
                VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                AttributeModifier.Operation.ADD_VALUE,
                SpellchargedGreatsword.DISPLAY_ATTACK_SPEED - 4.0D + expectedSpeedBonus,
                message + " attack speed"
        );
    }

    private static void assertSpellchargedGreatswordEntityReach(
            GameTestHelper helper,
            SpellchargedGreatsword item,
            ItemStack stack,
            double expectedReachBonus,
            String message
    ) {
        var modifiers = toModifierMultimap(item.getDefaultAttributeModifiers(stack));
        assertSingleModifierAmount(
                helper,
                modifiers.get(Attributes.ENTITY_INTERACTION_RANGE),
                AttributeModifier.Operation.ADD_VALUE,
                expectedReachBonus,
                message
        );
    }

    private static void assertSpellchargedGreatswordSweepingDamageRatio(
            GameTestHelper helper,
            ItemStack stack,
            double expectedRatio,
            String message
    ) {
        var actualSlotRatio = sumEffectiveModifierAmount(
                stack,
                EquipmentSlot.MAINHAND,
                Attributes.SWEEPING_DAMAGE_RATIO,
                AttributeModifier.Operation.ADD_VALUE
        );
        helper.assertTrue(Math.abs(actualSlotRatio - expectedRatio) < 1.0e-9D,
                message + " for main hand slot: expected " + expectedRatio + " but got " + actualSlotRatio);

        var actualMainHandGroupRatio = sumEffectiveModifierAmount(
                stack,
                EquipmentSlotGroup.MAINHAND,
                Attributes.SWEEPING_DAMAGE_RATIO,
                AttributeModifier.Operation.ADD_VALUE
        );
        helper.assertTrue(Math.abs(actualMainHandGroupRatio - expectedRatio) < 1.0e-9D,
                message + " for main hand tooltip group: expected " + expectedRatio + " but got "
                        + actualMainHandGroupRatio);

        var actualAnyGroupRatio = sumEffectiveModifierAmount(
                stack,
                EquipmentSlotGroup.ANY,
                Attributes.SWEEPING_DAMAGE_RATIO,
                AttributeModifier.Operation.ADD_VALUE
        );
        helper.assertTrue(Math.abs(actualAnyGroupRatio) < 1.0e-9D,
                message + " should not appear in the generic equipped tooltip group: got " + actualAnyGroupRatio);
    }

    private static double sumEffectiveModifierAmount(
            ItemStack stack,
            EquipmentSlot slot,
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
        var total = new double[1];
        stack.forEachModifier(slot, (actualAttribute, modifier) -> {
            if (actualAttribute.equals(attribute) && modifier.operation() == operation) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static double sumEffectiveModifierAmount(
            ItemStack stack,
            EquipmentSlotGroup slotGroup,
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
        var total = new double[1];
        stack.forEachModifier(slotGroup, (actualAttribute, modifier) -> {
            if (actualAttribute.equals(attribute) && modifier.operation() == operation) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static void assertAabbClose(GameTestHelper helper, AABB actual, AABB expected, String message) {
        helper.assertTrue(Math.abs(actual.minX - expected.minX) < 1.0e-9D
                        && Math.abs(actual.minY - expected.minY) < 1.0e-9D
                        && Math.abs(actual.minZ - expected.minZ) < 1.0e-9D
                        && Math.abs(actual.maxX - expected.maxX) < 1.0e-9D
                        && Math.abs(actual.maxY - expected.maxY) < 1.0e-9D
                        && Math.abs(actual.maxZ - expected.maxZ) < 1.0e-9D,
                message + ": expected " + expected + " but got " + actual);
    }

    private static double readBetterCombatRangeBonus(Object attributes) throws ReflectiveOperationException {
        // 1.21.1ではattackRangeはdeprecatedでrangeBonusに置き換わっている.
        return ((Number) attributes.getClass().getMethod("rangeBonus").invoke(attributes)).doubleValue();
    }

    private static int readBetterCombatAttackCount(Object attributes) throws ReflectiveOperationException {
        return ((Object[]) attributes.getClass().getMethod("attacks").invoke(attributes)).length;
    }

    private static boolean hasExpectedSpellchargedGreatswordEpicFightSkills(Object player, ItemStack stack)
            throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return (boolean) compatClass.getMethod(
                "hasExpectedSpellchargedGreatswordSkills",
                ServerPlayer.class,
                ItemStack.class
        ).invoke(null, player, stack);
    }

    private static boolean setEpicFightSweepingEdgeCharge(Object player, int stack)
            throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return (boolean) compatClass.getMethod(
                "setSweepingEdgeCharge",
                ServerPlayer.class,
                int.class
        ).invoke(null, player, stack);
    }

    private static int getEpicFightSweepingEdgeCharge(Object player) throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return ((Number) compatClass.getMethod(
                "getSweepingEdgeCharge",
                ServerPlayer.class
        ).invoke(null, player)).intValue();
    }

    private static int getEpicFightSweepingEdgeMaxStack() throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return ((Number) compatClass.getMethod("getSweepingEdgeMaxStack").invoke(null)).intValue();
    }

    private static void assertSpellchargedGreatswordEpicFightOverchargedCollider(
            GameTestHelper helper,
            Item item,
            String message
    ) throws ReflectiveOperationException {
        var collider = getSpellchargedGreatswordOverchargedEpicFightCollider(item);
        helper.assertTrue(collider != null, message + " should be created from Epic Fight collider API");

        var serialized = serializeEpicFightCollider(collider);
        helper.assertTrue(serialized.getInt("number") == 3,
                message + " should keep the Epic Fight greatsword multi collider count");

        var size = serialized.getList("size", 6);
        assertDouble(helper, size.getDouble(0), 0.5D, message + " size x");
        assertDouble(helper, size.getDouble(1), 0.8D, message + " size y");
        assertDouble(helper, size.getDouble(2), 1.5D, message + " size z");

        var center = serialized.getList("center", 6);
        assertDouble(helper, center.getDouble(0), 0.0D, message + " center x");
        assertDouble(helper, center.getDouble(1), 0.0D, message + " center y");
        assertDouble(helper, center.getDouble(2), -1.5D, message + " center z");
    }

    private static Object getSpellchargedGreatswordOverchargedEpicFightCollider(Item item)
            throws ReflectiveOperationException {
        var compatClass = Class.forName(
                "jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat"
        );
        return compatClass.getDeclaredMethod("getOverchargedWeaponCollider", Item.class).invoke(null, item);
    }

    private static CompoundTag serializeEpicFightCollider(Object collider) throws ReflectiveOperationException {
        var multiColliderClass = collider.getClass().getSuperclass();
        var colliderClass = multiColliderClass.getSuperclass();
        var number = ((Number) readField(multiColliderClass, collider, "numberOfColliders")).intValue();
        var center = (Vec3) readField(colliderClass, collider, "modelCenter");
        var colliders = (List<?>) readField(multiColliderClass, collider, "colliders");
        var firstObbCollider = colliders.getFirst();
        var modelVertices = (Object[]) readField(firstObbCollider.getClass(), firstObbCollider, "modelVertices");
        var size = (Vec3) modelVertices[1];

        var result = new CompoundTag();
        result.putInt("number", number);
        result.put("center", doubleList(center.x, center.y, center.z));
        result.put("size", doubleList(size.x, size.y, size.z));
        return result;
    }

    private static Object readField(Class<?> owner, Object target, String name) throws ReflectiveOperationException {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static ListTag doubleList(double... values) {
        var list = new ListTag();
        for (double value : values) {
            list.add(DoubleTag.valueOf(value));
        }
        return list;
    }

    private static void assertDouble(GameTestHelper helper, double actualValue, double expected, String message) {
        helper.assertTrue(Math.abs(actualValue - expected) < 1.0e-9D,
                message + ": expected " + expected + " but got " + actualValue);
    }

    private static void invokeSpellchargedGreatswordEpicFightTick(Object player) throws ReflectiveOperationException {
        var eventClass = Class.forName(
                "jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatswordEpicFightEvents"
        );
        eventClass.getMethod("onPlayerTick", PlayerTickEvent.Post.class).invoke(
                null,
                new PlayerTickEvent.Post((Player) player)
        );
    }

    private static Set<ResourceLocation> expectedSpellchargedGreatswordEnchantments(
            RegistryAccess registryAccess,
            ItemStack stack
    ) {
        var expectedEnchantments = new LinkedHashSet<>(collectAllowedEnchantments(
                registryAccess,
                enchantment -> enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD))
        ));
        expectedEnchantments.add(Enchantments.WISDOM.location());
        addExpectedMalumMagicCapableWeaponEnchantmentsIfPresent(stack, expectedEnchantments);
        addExpectedMalumSpiritPlunderIfPresent(stack, expectedEnchantments);
        return expectedEnchantments;
    }
}
