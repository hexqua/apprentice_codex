package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleCalibration;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleEnergy;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleFireworkBoost;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ShootingStarMantleForgeGameTests extends ApprenticeCodexGameTestScenarios {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ShootingStarMantleForgeGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void energyPersistsAndRecoveryRequiresFullPayment(GameTestHelper helper) {
        var player = equippedPlayer(helper, "mantle_energy");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        helper.assertTrue(MantleEnergy.read(stack).energy() == 100 && !stack.isDamageableItem(),
                "New mantle must have 100 energy and no durability");
        new MantleEnergy(1, false, 19).save(stack);
        var restored = ItemStack.of(stack.save(new CompoundTag()));
        var exhausted = MantleEnergy.read(restored).tickUse();
        exhausted.save(restored);
        helper.assertTrue(MantleEnergy.read(restored).energy() == 0 && MantleEnergy.read(restored).recovering(),
                "Depletion and recovery lock must survive NBT serialization");
        MagicData.getPlayerMagicData(player).setMana(19);
        new MantleEnergy(90, true, 0).save(stack);
        helper.assertFalse(ShootingStarMantleRuntime.recharge(player, stack, 20),
                "Insufficient mana must not partially pay recovery");
        helper.assertTrue(MantleEnergy.read(stack).energy() == 90, "Failed payment must preserve energy");
        MagicData.getPlayerMagicData(player).setMana(100);
        helper.assertTrue(ShootingStarMantleRuntime.recharge(player, stack, 100),
                "Full recovery cost must unlock the mantle");
        helper.assertTrue(MantleEnergy.read(stack).usable(), "Full recovery must clear the lock");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calibrationPreservesInactiveScrollsAndBonus(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        var mantle = (ShootingStarMantle) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, spell.getMaxLevel(), scroll);
        MantleCalibration.setScroll(stack, 0, scroll, lookup);
        helper.assertTrue(MantleCalibration.enabledSlots(stack, lookup) == 0,
                "A new mantle must keep inserted scrolls inactive");
        helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 0,
                new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                "Parchment must open one scroll slot");
        helper.assertTrue(MantleCalibration.enabledSlots(stack, lookup) == 1,
                "Parchment must enable the stored scroll");
        stack.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), 1);
        var raw = MantleCalibration.readSpell(stack, 0, lookup);
        helper.assertTrue(raw.getLevel() == spell.getMaxLevel(), "Saved scroll must keep its original level");
        var resolved = MantleCalibration.tooltipData(stack, lookup).entries().get(0).spell();
        helper.assertTrue(resolved.getLevel() == raw.getLevel() + 1,
                "Transcendence must add one level only to the visible spell");
        mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        helper.assertTrue(MantleCalibration.enabledSlots(stack, lookup) == 0
                        && ItemStack.isSameItemSameTags(MantleCalibration.getScroll(stack, 0, lookup), scroll),
                "Removing parchment must preserve the stored scroll");
        helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                "Mantle must not project a spell container onto itself");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void schoolRunesRemainExclusiveAndEnergyClamps(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        var mantle = (ShootingStarMantle) stack.getItem();
        helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE.get())),
                "First school rune must be accepted");
        helper.assertFalse(mantle.trySetCalibrationAdjustment(stack, 1,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())),
                "School runes must exclude each other");
        mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get())),
                "Mana rune must raise capacity");
        new MantleEnergy(160, false, 0, 200).save(stack);
        mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        helper.assertTrue(MantleEnergy.read(stack).energy() == 100 && MantleEnergy.read(stack).maxEnergy() == 100,
                "Removing mana rune must clamp surplus energy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void impulseRejectsInvalidAndRepeatedInput(GameTestHelper helper) {
        var player = equippedPlayer(helper, "mantle_impulse");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Equipped mantle must enter hover");
        new MantleEnergy(11, false, 0).save(stack);
        helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 1, Float.NaN, 0),
                "Non-finite input must be rejected");
        helper.assertTrue(MantleEnergy.read(stack).energy() == 11, "Invalid input must not spend energy");
        helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 2, 1, 0),
                "Finite input must be accepted");
        helper.assertTrue(MantleEnergy.read(stack).energy() == 1,
                "Accepted impulse must spend exactly ten energy");
        helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 2, 1, 0),
                "Duplicate sequence must be rejected");
        helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 3, 1, 0),
                "Impulse must not restart during its active window");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mantleStartsFlightWithoutChestElytra(GameTestHelper helper) {
        var player = equippedPlayer(helper, "mantle_flight");
        helper.assertTrue(player.tryToStartFallFlying(),
                "Mantle must start fall flight without chest Elytra");
        player.aiStep();
        helper.assertTrue(player.isFallFlying(),
                "Mantle must maintain fall flight while equipped");
        helper.assertTrue(ShootingStarMantleRuntime.toggle(player),
                "Mantle must switch from flight to hover");
        helper.assertFalse(player.isFallFlying(), "Hover must stop fall flight");
        helper.assertFalse(player.tryToStartFallFlying(), "Hover must block flight restart");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fireworkPrefersSafeLongestOwnedRocket(GameTestHelper helper) {
        var player = equippedPlayer(helper, "mantle_firework");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        mantle.trySetCalibrationAdjustment(stack, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.EVOCATION_RUNE.get()));
        var shelf = player.getCapability(Capabilities.PERSONAL_INVENTORY)
                .orElseThrow(() -> new IllegalStateException("Missing personal inventory for mantle GameTest"))
                .getHandler();
        shelf.setStackInSlot(0, rocket(3, 2, true));
        shelf.setStackInSlot(1, rocket(1, 2, false));
        shelf.setStackInSlot(2, rocket(3, 2, false));
        player.getInventory().setItem(0, rocket(3, 2, false));
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(shelf.getStackInSlot(0).getCount() == 2
                        && shelf.getStackInSlot(1).getCount() == 2
                        && shelf.getStackInSlot(2).getCount() == 1,
                "Flight must consume the longest safe rocket from personal inventory");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 2,
                "Later inventory sources must stay untouched");
        helper.assertTrue(MantleFireworkBoost.countRockets(player) == 5,
                "Remaining count must exclude explosive rockets");
        helper.assertTrue(player.level().getEntitiesOfClass(FireworkRocketEntity.class,
                        player.getBoundingBox().inflate(4)).size() == 1,
                "Flight start must create one rocket");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blinkImmunityEndsAfterCancellation(GameTestHelper helper) {
        var player = equippedPlayer(helper, "mantle_blink");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        mantle.trySetCalibrationAdjustment(stack, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ENDER_RUNE.get()));
        helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Mantle must enter hover");
        helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 1, 1, 0),
                "Ender rune must allow an impulse");
        var protectedHit = new LivingAttackEvent(player, helper.getLevel().damageSources().generic(), 2);
        helper.assertTrue(MantleBlink.cancelIncomingDamageIfInvulnerable(protectedHit)
                        && protectedHit.isCanceled(),
                "Active blink must reject incoming damage");
        ShootingStarMantleRuntime.state(player).blink.cancel();
        var unprotectedHit = new LivingAttackEvent(player, helper.getLevel().damageSources().generic(), 2);
        helper.assertFalse(MantleBlink.cancelIncomingDamageIfInvulnerable(unprotectedHit)
                        || unprotectedHit.isCanceled(),
                "Canceled blink must not keep damage immunity");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    private static ItemStack rocket(int flight, int count, boolean explosion) {
        var stack = new ItemStack(Items.FIREWORK_ROCKET, count);
        var fireworks = new CompoundTag();
        fireworks.putByte("Flight", (byte) flight);
        if (explosion) {
            var explosions = new ListTag();
            explosions.add(new CompoundTag());
            fireworks.put("Explosions", explosions);
        }
        stack.getOrCreateTag().put("Fireworks", fireworks);
        return stack;
    }

    private static FakePlayer equippedPlayer(GameTestHelper helper, String name) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 1), name);
        CuriosApi.getCuriosInventory(player).resolve().orElseThrow().setEquippedCurio(
                CuriosSlotConstants.BACK, 0, new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get()));
        player.setOnGround(false);
        ShootingStarMantleRuntime.refreshEquipment(player);
        return player;
    }
}
