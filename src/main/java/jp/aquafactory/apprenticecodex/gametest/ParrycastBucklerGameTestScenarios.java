package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.shield.ImbueShieldBlockCastEvent;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.ArrayList;
import java.util.UUID;

final class ParrycastBucklerGameTestScenarios {
    private ParrycastBucklerGameTestScenarios() {}

    static void parrycastBucklerKeepsCoreContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            var item = (ParrycastBuckler) stack.getItem();
            var tooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
            assertTooltipKeyAt(helper, tooltipLines, 0,
                    "item.apprenticecodex.parrycast_buckler.desc");
            assertTooltipKeyAt(helper, tooltipLines, 1,
                    "item.apprenticecodex.parrycast_buckler.cast_default");
            helper.assertTrue(stack.getMaxDamage() == 1561, "Parrycast Buckler durability should be 1561");
            helper.assertTrue(item.getEnchantmentValue(stack) == 22, "Parrycast Buckler enchantment value should be 22");
            helper.assertTrue(item.isValidRepairItem(stack,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())),
                    "Parrycast Buckler should repair with arcane ingot");
            helper.assertFalse(item.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)),
                    "Parrycast Buckler should not repair with diamond");
            var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.UNBREAKING)), "Parrycast should accept shield enchantments");
            helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.TENSE)), "Parrycast should accept Tense");
            helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.ALACRITY)), "Parrycast should accept Alacrity");
            helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.TRANSCENDENCE)), "Parrycast should accept Transcendence");
            helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.WISDOM)), "Parrycast should accept Wisdom");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.SENSE_EVIL.get(), 1), "Parrycast should accept instant no-recast spells");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.MANTIS_LEAP.get(), 1),
                    "Parrycast should always accept long no-recast spells for imbue");
            helper.assertFalse(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1), "Long spell should require Silver Ring");
            var longScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(SpellRegistry.MANTIS_LEAP.get(), 1, longScroll);
            helper.assertTrue(SpellCalibrationImbueHelper.canPlaceScrollAt(stack, 0, longScroll),
                    "Long scroll placement should not require Silver Ring");
            helper.assertTrue(SpellCalibrationImbueHelper.setScrollAt(stack, 0, longScroll),
                    "Long scroll should be insertable without Silver Ring");
            helper.assertTrue(item.isMismatchedCastConditionAt(stack, 0),
                    "Inserted long spell should warn while Silver Ring is absent");
            assertFirstRestrictionKey(helper, item.getImbueRestrictionTooltipLines(stack),
                    "item.apprenticecodex.spellgun.tooltip.restrict_restrict_instant_only");
            ParrycastBuckler.setCalibrationAdjustment(stack, 0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            helper.assertTrue(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1), "Silver Ring should allow long spells");
            helper.assertFalse(item.isMismatchedCastConditionAt(stack, 0),
                    "Silver Ring should clear the inserted long spell warning");
            assertFirstRestrictionKey(helper, item.getImbueRestrictionTooltipLines(stack),
                    "item.apprenticecodex.spellgun.tooltip.restrict_restrict_not_continuous");
        });
    }

    static void parrycastBucklerSupportsThreeAdjustmentsAndSchoolDeduplication(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "parrycast_calibration_test");
            var stack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);
            for (int i = 0; i < 3; i++) helper.assertTrue(menu.isAdjustmentSlotEnabled(i), "Parrycast adjustment slot should be enabled: " + i);

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            ParrycastBuckler.setCalibrationAdjustment(stack, 0, fireRune);
            ParrycastBuckler.setCalibrationAdjustment(stack, 1, fireRune);
            ParrycastBuckler.setCalibrationAdjustment(stack, 2, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
            helper.assertTrue(ParrycastBuckler.hasWisdomShard(stack), "Wisdom Shard should be stored");
            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
            assertTooltipKeyAt(helper, tooltipLines, 0,
                    "item.apprenticecodex.parrycast_buckler.desc");
            assertTooltipKeyAt(helper, tooltipLines, 1,
                    "item.apprenticecodex.parrycast_buckler.cast_wisdom");
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(fireRune).orElse(null);
            var power = MagicTools.resolveSchoolPowerAttribute(school);
            long matching = stack.getAttributeModifiers().modifiers().stream()
                    .filter(entry -> entry.slot().equals(EquipmentSlotGroup.OFFHAND)
                            && entry.attribute().value() == power
                            && entry.modifier().amount() == 0.1D
                            && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE).count();
            helper.assertTrue(matching == 1, "Duplicate school runes should grant one school power modifier");
            helper.assertTrue(stack.getAttributeModifiers().modifiers().stream()
                            .noneMatch(entry -> entry.slot().equals(EquipmentSlotGroup.MAINHAND)
                                    && entry.attribute().value() == power),
                    "School rune power should be limited to offhand");
        });
    }

    static void parrycastBucklerKeepsPerfectGuardWindowAndDurabilityRateLimit(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "parrycast_guard_test");
        var stack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardTicks() == 10,
                "Parrycast perfect guard config should default to ten ticks");
        helper.assertTrue(ParrycastBuckler.isPerfectGuard(player), "Use start should enter perfect guard window");
        helper.assertTrue(ParrycastBuckler.resolveDurabilityCost(12.0F, true) == 1, "Perfect guard should cap durability cost at one");
        helper.assertTrue(ParrycastBuckler.resolveDurabilityCost(12.0F, false) == 13, "Normal guard should keep vanilla durability cost");
        ParrycastBuckler.rememberDurabilityConsumed(stack, 100L);
        helper.assertTrue(ParrycastBuckler.isDurabilitySuppressed(stack, 110L), "Durability should be suppressed through tick ten");
        helper.assertFalse(ParrycastBuckler.isDurabilitySuppressed(stack, 111L), "Durability suppression should expire after tick ten");
        helper.assertTrue(ParrycastBuckler.resolveCooldownReductionTicks(101, 40) == 11,
                "Known maximum cooldown should reduce by a rounded-up ten percent");
        helper.assertTrue(ParrycastBuckler.resolveCooldownReductionTicks(0, 21) == 5,
                "Unknown maximum cooldown should reduce rounded-up twenty percent of remaining time");
        helper.assertTrue(stack.get(DataComponents.CUSTOM_DATA) == null
                        || !stack.get(DataComponents.CUSTOM_DATA).copyTag().contains("ApprenticeCodexParrycastBucklerAnimationState"),
                "Animation state should not be persisted in the item stack");

        helper.runAfterDelay(11, () -> {
            helper.assertFalse(ParrycastBuckler.isPerfectGuard(player),
                    "Perfect guard window should expire without an animation-driven use restart");
            var event = new LivingShieldBlockEvent(player,
                    new DamageContainer(helper.getLevel().damageSources().generic(), 4.0F), true);
            ImbueShieldBlockCastEvent.onParrycastBucklerBlock(event);
            helper.assertFalse(player.isUsingItem(), "Normal guard should stop Parrycast Buckler use");
            helper.assertTrue(player.getCooldowns().isOnCooldown(stack.getItem()),
                    "Normal guard should apply the release cooldown before stopping use");
            player.gameMode.useItem(player, helper.getLevel(), stack, InteractionHand.OFF_HAND);
            helper.assertFalse(player.isUsingItem(),
                    "Held use input should not restart Parrycast Buckler while the cooldown is active");
            helper.assertTrue(stack.get(DataComponents.CUSTOM_DATA) == null
                            || !stack.get(DataComponents.CUSTOM_DATA).copyTag().contains("ApprenticeCodexParrycastBucklerAnimationState"),
                    "Stopping use should not persist animation state in the item stack");
            helper.succeed();
        });
    }

    static void parrycastBucklerBlocksDispenserArrowAndStopsOnUnblockedDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockedPlayer = createDamageableTestPlayer(
                    helper, new BlockPos(0, 2, 0), "parrycast_dispenser_arrow_block_test");
            helper.getLevel().addFreshEntity(blockedPlayer);
            blockedPlayer.setYRot(0.0F);
            blockedPlayer.setXRot(0.0F);
            var blockedStack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            var bloodSlash = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BLOOD_SLASH_SPELL.get();
            helper.assertTrue(SpellCalibrationImbueHelper.setScrollAt(
                            blockedStack, 0, BowGameTestSupport.createSpellScroll(bloodSlash)),
                    "Parrycast Buckler should accept Blood Slash for the perfect-guard test");
            blockedPlayer.setItemInHand(InteractionHand.OFF_HAND, blockedStack);
            var magicData = MagicData.getPlayerMagicData(blockedPlayer);
            helper.assertTrue(magicData != null, "Blood Slash perfect-guard test requires MagicData");
            magicData.setMana(0.0F);
            blockedStack.getItem().use(helper.getLevel(), blockedPlayer, InteractionHand.OFF_HAND);
            helper.assertTrue(blockedPlayer.isBlocking(),
                    "Parrycast Buckler should enter its blocking state immediately");

            var frontalArrow = new Arrow(EntityType.ARROW, helper.getLevel());
            frontalArrow.setPos(blockedPlayer.getX(), blockedPlayer.getY() + 1.0D, blockedPlayer.getZ() + 3.0D);
            helper.getLevel().addFreshEntity(frontalArrow);
            var healthBeforeBlock = blockedPlayer.getHealth();
            var bloodSlashCountBefore = helper.getLevel().getEntitiesOfClass(
                    BloodSlashProjectile.class, blockedPlayer.getBoundingBox().inflate(32.0D)).size();
            blockedPlayer.hurt(helper.getLevel().damageSources().arrow(frontalArrow, null), 4.0F);
            helper.assertTrue(blockedPlayer.getHealth() == healthBeforeBlock,
                    "Parrycast Buckler should block a frontal dispenser arrow");
            helper.assertTrue(blockedPlayer.isUsingItem(),
                    "A perfect guard against a dispenser arrow should keep the buckler raised");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                            BloodSlashProjectile.class, blockedPlayer.getBoundingBox().inflate(32.0D)).size()
                            > bloodSlashCountBefore,
                    "A perfect guard should cast the imbued Blood Slash immediately");
            helper.assertFalse(magicData.isCasting(),
                    "An instant perfect-guard spell should not leave pending casting state");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(bloodSlash),
                    "A perfect-guard Blood Slash should apply its spell cooldown");
            helper.assertTrue(magicData.getMana() == 0.0F,
                    "A perfect-guard Blood Slash should not consume mana");

            var damagedPlayer = createDamageableTestPlayer(
                    helper, new BlockPos(3, 2, 0), "parrycast_unblocked_damage_release_test");
            helper.getLevel().addFreshEntity(damagedPlayer);
            damagedPlayer.setYRot(0.0F);
            damagedPlayer.setXRot(0.0F);
            var damagedStack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            damagedPlayer.setItemInHand(InteractionHand.OFF_HAND, damagedStack);
            damagedStack.getItem().use(helper.getLevel(), damagedPlayer, InteractionHand.OFF_HAND);

            var healthBeforeDamage = damagedPlayer.getHealth();
            var hurtResult = damagedPlayer.hurt(helper.getLevel().damageSources().generic(), 4.0F);
            helper.assertTrue(hurtResult && damagedPlayer.getHealth() < healthBeforeDamage,
                    "Damage that bypasses Parrycast Buckler should reduce health");
            helper.assertFalse(damagedPlayer.isUsingItem(),
                    "Damage that bypasses Parrycast Buckler should force the guard stance to end");
        });
    }

    static void parrycastWisdomOnlyReducesAllCooldownsWhenSelectedSpellIsCoolingDown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var otherSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();

            var ready = createWisdomParryContext(
                    helper,
                    new BlockPos(0, 2, 0),
                    "parrycast_wisdom_ready_test",
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(ready.player(), otherSpell, ready.castSource());
            int readyOtherBefore = cooldownRemaining(ready.magicData(), otherSpell);
            triggerPerfectGuard(ready);
            helper.assertTrue(cooldownRemaining(ready.magicData(), otherSpell) == readyOtherBefore,
                    "Wisdom should not reduce other cooldowns while the selected spell is ready");

            var invalid = createWisdomParryContext(
                    helper,
                    new BlockPos(2, 2, 0),
                    "parrycast_wisdom_invalid_test",
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(invalid.player(), otherSpell, invalid.castSource());
            int invalidOtherBefore = cooldownRemaining(invalid.magicData(), otherSpell);
            triggerPerfectGuard(invalid);
            helper.assertTrue(cooldownRemaining(invalid.magicData(), otherSpell) == invalidOtherBefore,
                    "Wisdom should not reduce other cooldowns when the selected spell cannot be used");

            var coolingDown = createWisdomParryContext(
                    helper,
                    new BlockPos(4, 2, 0),
                    "parrycast_wisdom_cooldown_test",
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(
                    coolingDown.player(), coolingDown.selectedSpell(), coolingDown.castSource()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(coolingDown.player(), otherSpell, coolingDown.castSource());
            int selectedBefore = cooldownRemaining(coolingDown.magicData(), coolingDown.selectedSpell());
            int otherBefore = cooldownRemaining(coolingDown.magicData(), otherSpell);
            triggerPerfectGuard(coolingDown);
            helper.assertTrue(cooldownRemaining(coolingDown.magicData(), coolingDown.selectedSpell()) < selectedBefore,
                    "Wisdom should reduce the selected spell cooldown while it is active");
            helper.assertTrue(cooldownRemaining(coolingDown.magicData(), otherSpell) < otherBefore,
                    "Wisdom should reduce all cooldowns when the selected spell is cooling down");
        });
    }

    private static WisdomParryContext createWisdomParryContext(
            GameTestHelper helper,
            BlockPos position,
            String name,
            AbstractSpell selectedSpell
    ) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, position, name);
        var buckler = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
        ParrycastBuckler.setCalibrationAdjustment(buckler, 0, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, BowGameTestSupport.createSpellScroll(selectedSpell));
        ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, buckler);
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Wisdom Parrycast test requires MagicData");
        magicData.setMana(1000.0F);
        magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));
        var selection = new SpellSelectionManager(player).getSelection();
        helper.assertTrue(selection != null && selection.spellData.getSpell() == selectedSpell,
                "Wisdom Parrycast test should resolve the selected offhand spell");
        return new WisdomParryContext(player, buckler, magicData, selectedSpell, selection.getCastSource());
    }

    private static void triggerPerfectGuard(WisdomParryContext context) {
        context.buckler().getItem().use(context.player().level(), context.player(), InteractionHand.MAIN_HAND);
        ((ParrycastBuckler) context.buckler().getItem()).handlePerfectGuard(
                context.player(), context.buckler(), InteractionHand.MAIN_HAND
        );
    }

    private static int cooldownRemaining(MagicData magicData, AbstractSpell spell) {
        var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
        return cooldown == null ? 0 : cooldown.getCooldownRemaining();
    }

    private static FakePlayer createDamageableTestPlayer(GameTestHelper helper, BlockPos position, String name) {
        var player = new DamageableFakePlayer(
                helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        // FakePlayer 固有の無敵と生成直後の保護を外し、実際の盾・ダメージ処理を検証する。
        try {
            var spawnProtection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true);
            spawnProtection.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to disable spawn protection for GameTest", exception);
        }
        var absolutePosition = helper.absolutePos(position);
        player.setPos(absolutePosition.getX() + 0.5D, absolutePosition.getY(), absolutePosition.getZ() + 0.5D);
        return player;
    }

    private static final class DamageableFakePlayer extends FakePlayer {
        private DamageableFakePlayer(net.minecraft.server.level.ServerLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return false;
        }
    }

    private record WisdomParryContext(
            ServerPlayer player,
            ItemStack buckler,
            MagicData magicData,
            AbstractSpell selectedSpell,
            io.redspace.ironsspellbooks.api.spells.CastSource castSource
    ) {
    }

    private static void assertFirstRestrictionKey(GameTestHelper helper, java.util.List<net.minecraft.network.chat.Component> lines,
                                                  String expectedKey) {
        helper.assertFalse(lines.isEmpty(), "Parrycast restriction tooltip should not be empty");
        var contents = lines.get(0).getContents();
        helper.assertTrue(contents instanceof TranslatableContents translatable && expectedKey.equals(translatable.getKey()),
                "Unexpected Parrycast restriction tooltip: " + lines.get(0));
    }

    private static void assertTooltipKeyAt(GameTestHelper helper, java.util.List<Component> lines, int index,
                                           String expectedKey) {
        helper.assertTrue(lines.size() > index, "Parrycast tooltip line is missing at index " + index);
        var contents = lines.get(index).getContents();
        helper.assertTrue(contents instanceof TranslatableContents translatable
                        && expectedKey.equals(translatable.getKey()),
                "Unexpected Parrycast tooltip at index " + index + ": " + lines.get(index));
    }
}
