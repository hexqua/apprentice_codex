package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleCalibration;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleEnergy;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.ENDER_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MantleBlinkGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private MantleBlinkGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void repeatedBlinkPreservesPreviousAppearanceDuringObserverInterpolation(GameTestHelper helper) {
        var blink = new MantleBlink();
        blink.accept(100, 0, 5, new Vec3(1, 0, 0));
        blink.accept(110, 1, 5, new Vec3(1, 0, 0));
        helper.assertTrue(blink.renderElapsed(108, 0.5F) == 8.5, "New activation must not erase the previous interpolated appearance");
        helper.assertTrue(blink.renderElapsed(110, 0.5F) == 0.5, "Observer must switch to the new disappearance when its render time arrives");
        helper.assertFalse(blink.active(108), "Rendering history must never extend server invulnerability");
        blink.cancel();
        helper.assertTrue(blink.renderElapsed(108, 0) < 0, "Cancellation must also discard previous visual history");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void schoolRunesExcludeEachOtherWithoutMergingDescriptions(GameTestHelper helper) {
        var player = player(helper, "blink_rules");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        for (var first : List.of(ICE_RUNE.get(), ENDER_RUNE.get())) {
            for (int slot = 0; slot < 3; slot++) {
                helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, slot, new ItemStack(first)), "First school rune must fit any slot");
                for (var other : List.of(ICE_RUNE.get(), ENDER_RUNE.get())) {
                    helper.assertFalse(mantle.trySetCalibrationAdjustment(stack, (slot + 1) % 3, new ItemStack(other)),
                            "School rune exclusivity must be symmetric and reject duplicates");
                }
                helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, slot, ItemStack.EMPTY), "Exclusive runes must remain removable");
            }
        }
        var rules = MantleCalibration.PROFILE.rules().stream()
                .filter(rule -> rule.accepts(new ItemStack(ICE_RUNE.get())) || rule.accepts(new ItemStack(ENDER_RUNE.get()))).toList();
        helper.assertTrue(rules.size() == 2 && !rules.get(0).displayId().equals(rules.get(1).displayId()), "Rune descriptions must remain separate");
        for (var rule : rules) {
            helper.assertTrue(rule.constraintDisplay().translationKey().orElseThrow()
                    .equals("jei.apprenticecodex.spell_calibration_bench.constraint.school_rune"), "Both rules must reuse the school rune restriction");
        }
        var blink = rules.stream().filter(rule -> rule.accepts(new ItemStack(ENDER_RUNE.get()))).findFirst().orElseThrow();
        helper.assertTrue(blink.effectLines().size() == 2, "Blink must have exactly two description lines");
        for (int i = 0; i < 2; i++) {
            helper.assertTrue(blink.effectLines().get(i).getContents() instanceof TranslatableContents text
                    && text.getKey().equals("jei.apprenticecodex.spell_calibration_bench.effect.change_mantle_blink_" + (i + 1)),
                    "Blink must retain the agreed translation keys");
        }
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(0).set(stack);
        menu.setCarried(new ItemStack(ICE_RUNE.get()));
        menu.clicked(1, 0, ClickType.PICKUP, player);
        menu.setCarried(new ItemStack(ENDER_RUNE.get()));
        menu.clicked(2, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ENDER_RUNE.get()) && menu.getSlot(2).getItem().isEmpty(), "Normal clicks must reject incompatible runes without consuming them");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(9, new ItemStack(ENDER_RUNE.get()));
        int inventorySlot = menu.slots.stream().filter(slot -> slot.container == player.getInventory() && slot.getContainerSlot() == 9)
                .findFirst().orElseThrow().index;
        menu.quickMoveStack(player, inventorySlot);
        helper.assertFalse(MantleCalibration.usesBlink(stack), "Shift-click must not bypass exclusivity");
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ICE_RUNE.get()), "Normal clicks must extract the existing rune");
        menu.setCarried(new ItemStack(ENDER_RUNE.get()));
        menu.clicked(2, 0, ClickType.PICKUP, player);
        helper.assertTrue(MantleCalibration.usesBlink(stack), "The other rune must fit after extraction");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blinkFreezesHeightMovesSixBlocksAndRestoresHoverHeight(GameTestHelper helper) {
        movement(helper, false, false);
    }

    @GameTest(template = TEMPLATE)
    public static void blinkStopsAtWallsWithoutSteppingUp(GameTestHelper helper) {
        movement(helper, true, false);
    }

    @GameTest(template = TEMPLATE)
    public static void diagonalBlinkKeepsSixBlockRange(GameTestHelper helper) {
        movement(helper, false, true);
    }

    private static void movement(GameTestHelper helper, boolean wall, boolean diagonal) {
        var player = player(helper, "blink_move_" + wall + "_" + diagonal);
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        ((ShootingStarMantle) stack.getItem()).trySetCalibrationAdjustment(stack, 0, new ItemStack(ENDER_RUNE.get()));
        // 隣のtestやtemplateの天井に依存せず、移動範囲をこの構造内に用意する。
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                for (int y = 1; y <= 5; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
        }
        var origin = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
        player.setPos(origin);
        player.setYRot(-90);
        if (wall) helper.setBlock(new BlockPos(2, 2, 0), Blocks.STONE);
        for (int tick = 0; tick < MantleBlink.DURATION; tick++) {
            int age = tick;
            helper.runAtTickTime(tick + 1, () -> {
                // 同tickに別callbackを登録すると検査が発動より先に走るため、開始と初回移動を一続きにする。
                if (age == 0) {
                    helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Blink test must enter hover");
                    player.setDeltaMovement(0.3, -0.2, 0.4);
                    helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 0, 1, diagonal ? -1 : 0), "Ender rune must accept blink");
                    helper.assertTrue(MantleEnergy.read(stack).energy() == 90, "Blink must cost the same ten energy as dash");
                    helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 1, 1, 0), "Blink must reject reentry");
                    helper.assertTrue(MantleEnergy.read(stack).energy() == 90, "Rejected input must not consume energy");
                }
                var state = ShootingStarMantleRuntime.state(player);
                ShootingStarMantleRuntime.tick(player);
                MantleMovement.travel(player, new Vec3(1, 1, 1), state);
                double distance = player.position().subtract(origin).horizontalDistance();
                double expected = Math.min(4, Math.max(0, age - 2)) * 1.5;
                helper.assertTrue(Math.abs(player.getY() - origin.y) < 1.0e-6,
                        "Blink must lock height through all phases: age=" + age + ", position=" + player.position()
                                + ", origin=" + origin + ", start=" + state.blink.start()
                                + ", gameTime=" + player.level().getGameTime() + ", hover=" + state.hovering);
                helper.assertTrue(wall ? distance < 2 : Math.abs(distance - expected) < 1.0e-5, "Blink must move only during its four hidden ticks");
                helper.assertTrue(state.blink.active(player.level().getGameTime()), "Blink must remain active for ten ticks");
                var damage = damage(player);
                helper.assertTrue(MantleBlink.cancelIncomingDamageIfInvulnerable(damage) && damage.isCanceled(), "Every blink phase must cancel incoming hits");
            });
        }
        helper.runAtTickTime(11, () -> {
            var state = ShootingStarMantleRuntime.state(player);
            ShootingStarMantleRuntime.tick(player);
            helper.assertFalse(MantleBlink.cancelIncomingDamageIfInvulnerable(damage(player)), "Invulnerability must end at tick ten");
            helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO) && state.movingTicks == 0 && state.lastPosition == null,
                    "Completion must clear momentum and movement history");
            MantleMovement.travel(player, Vec3.ZERO, state);
            helper.assertTrue(state.movingTicks == 0 && player.getDeltaMovement().y > 0, "Hover must rise toward five blocks after blink");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void depletedBlinkFinishesAndDoesNotSpendDefensiveMana(GameTestHelper helper) {
        var player = player(helper, "blink_depleted");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        ((ShootingStarMantle) stack.getItem()).trySetCalibrationAdjustment(stack, 0, new ItemStack(ENDER_RUNE.get()));
        ApprenticeCodexGameTestScenarios.equipCurio(player, "charm", new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
        new MantleEnergy(1, false, 0).save(stack);
        helper.runAtTickTime(1, () -> {
            ShootingStarMantleRuntime.toggle(player);
            helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 0, 1, 0), "Last energy must allow a complete blink");
            var magic = MagicData.getPlayerMagicData(player);
            magic.setMana(100);
            var damage = ApprenticeCodexGameTestScenarios.postLivingAttackEventForGameTest(player, player.damageSources().generic(), 5);
            helper.assertTrue(damage.isCanceled() && magic.getMana() == 100, "Blink immunity must precede defensive mana consumption");
        });
        for (int tick = 1; tick <= 9; tick++) {
            helper.runAtTickTime(tick + 1, () -> {
                ShootingStarMantleRuntime.tick(player);
                helper.assertTrue(ShootingStarMantleRuntime.isHovering(player), "Paid blink must survive depletion until its final tick");
            });
        }
        helper.runAtTickTime(11, () -> {
            ShootingStarMantleRuntime.tick(player);
            helper.assertFalse(ShootingStarMantleRuntime.isHovering(player), "Depleted hover must stop after blink completes");
            helper.assertTrue(MantleEnergy.read(stack).energy() == 0, "Blink must not regenerate before completion");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void blinkCancellationClearsImmunityAndRejectsInvalidInput(GameTestHelper helper) {
        var player = player(helper, "blink_cancel");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        ((ShootingStarMantle) stack.getItem()).trySetCalibrationAdjustment(stack, 0, new ItemStack(ENDER_RUNE.get()));
        ShootingStarMantleRuntime.toggle(player);
        helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 0, Float.NaN, 0), "Invalid direction must not start blink");
        helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 1, 1, 0), "Valid input must start blink");
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", ItemStack.EMPTY);
        ShootingStarMantleRuntime.refreshEquipment(player);
        helper.assertFalse(MantleBlink.cancelIncomingDamageIfInvulnerable(damage(player)), "Unequip must immediately revoke immunity");
        helper.assertTrue(ShootingStarMantleRuntime.state(player).blink.start() < 0, "Cancellation must clear visual state");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    private static LivingIncomingDamageEvent damage(FakePlayer player) {
        return new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().generic(), 5));
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(1, 3, 1), name);
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get()));
        ShootingStarMantleRuntime.refreshEquipment(player);
        return player;
    }
}
