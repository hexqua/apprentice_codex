package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleEnergy;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleFireworkBoost;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MantleFireworkGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private MantleFireworkGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void flightStartPrefersSafeLongestRocketInShelf(GameTestHelper helper) {
        var player = player(helper, "mantle_firework_shelf");
        var shelf = Capabilities.getPersonalInventory(player).orElseThrow().getHandler();
        shelf.setStackInSlot(0, rocket(3, 2, true));
        shelf.setStackInSlot(1, rocket(1, 2, false));
        shelf.setStackInSlot(2, rocket(3, 2, false));
        player.getEnderChestInventory().setItem(0, rocket(3, 2, false));
        player.getInventory().setItem(9, rocket(3, 2, false));
        player.getInventory().setItem(0, rocket(3, 2, false));
        player.getInventory().offhand.set(0, rocket(3, 2, false));
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(shelf.getStackInSlot(0).getCount() == 2 && shelf.getStackInSlot(1).getCount() == 2
                        && shelf.getStackInSlot(2).getCount() == 1, "Safe longest shelf rocket must be consumed first");
        helper.assertTrue(player.getEnderChestInventory().getItem(0).getCount() == 2
                        && player.getInventory().getItem(9).getCount() == 2, "Later inventories must not be consumed");
        helper.assertTrue(MantleFireworkBoost.countRockets(player) == 11, "Remaining count must include every safe rocket once");
        helper.assertTrue(rockets(player) == 1 && MantleEnergy.read(ShootingStarMantleRuntime.findEquipped(player)).energy() == 100,
                "Flight start must spawn one rocket without extra energy cost");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void inputUsesSourceOrderAndRequiresFreshPress(GameTestHelper helper) {
        var player = player(helper, "mantle_firework_input");
        var shelf = Capabilities.getPersonalInventory(player).orElseThrow().getHandler();
        shelf.setStackInSlot(0, rocket(1, 1, true));
        player.getEnderChestInventory().setItem(0, rocket(1, 1, false));
        player.getInventory().setItem(9, rocket(3, 1, false));
        player.getInventory().setItem(0, rocket(3, 1, false));
        player.getInventory().offhand.set(0, rocket(3, 1, false));
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(player.getEnderChestInventory().getItem(0).isEmpty(), "Unsafe shelf must be skipped for ender chest");
        var boost = ShootingStarMantleRuntime.state(player).firework;
        boost.input(player, 0, true);
        boost.input(player, 1, true);
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 1 && rockets(player) == 1,
                "Start press and held input must not launch twice in one tick");
        helper.runAfterDelay(1, () -> {
            boost.input(player, 2, true);
            helper.assertTrue(rockets(player) == 1, "Held input must not launch another rocket");
            boost.input(player, 3, false);
            boost.input(player, 4, true);
            helper.assertTrue(player.getInventory().getItem(9).isEmpty() && rockets(player) == 2,
                    "New press must consume inventory before hotbar");
            boost.input(player, 4, false);
            helper.assertTrue(rockets(player) == 2, "Repeated sequence must be ignored");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void creativeFlightRequiresButDoesNotConsumeRocket(GameTestHelper helper) {
        var player = player(helper, "mantle_firework_creative");
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        player.getInventory().offhand.set(0, rocket(1, 1, false));
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(rockets(player) == 1 && player.getInventory().offhand.get(0).getCount() == 1,
                "Creative launch must require a rocket without consuming it");
        ShootingStarMantleRuntime.clear(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hotbarThenOffhandAndNoStock(GameTestHelper helper) {
        var player = player(helper, "mantle_firework_hand_fallback");
        player.getInventory().selected = 2;
        player.getInventory().setItem(2, rocket(1, 1, false));
        player.getInventory().offhand.set(0, rocket(3, 1, false));
        player.startFallFlying();
        ShootingStarMantleRuntime.tick(player);
        helper.assertTrue(player.getInventory().getItem(2).isEmpty() && rockets(player) == 1
                        && MantleFireworkBoost.countRockets(player) == 1,
                "Selected main hand must be searched and counted once within hotbar");
        helper.runAfterDelay(1, () -> {
            var boost = ShootingStarMantleRuntime.state(player).firework;
            boost.input(player, 0, true);
            helper.assertTrue(player.getInventory().offhand.get(0).isEmpty() && rockets(player) == 2,
                    "Offhand must supply the next rocket");
            boost.input(player, 1, false);
        });
        helper.runAfterDelay(2, () -> {
            ShootingStarMantleRuntime.state(player).firework.input(player, 2, true);
            helper.assertTrue(rockets(player) == 2 && MantleFireworkBoost.countRockets(player) == 0,
                    "No stock must leave flight active without spawning a rocket");
            helper.assertTrue(player.isFallFlying(), "Empty stock must not stop mantle flight");
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 2, 1))));
        player.setOnGround(false);
        var mantle = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        ((ShootingStarMantle) mantle.getItem()).trySetCalibrationAdjustment(mantle, 0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.EVOCATION_RUNE.get()));
        ApprenticeCodexGameTestScenarios.equipCurio(player, "back", mantle);
        ShootingStarMantleRuntime.refreshEquipment(player);
        return player;
    }

    private static ItemStack rocket(int duration, int count, boolean explosion) {
        var stack = new ItemStack(Items.FIREWORK_ROCKET, count);
        stack.set(DataComponents.FIREWORKS, new Fireworks(duration,
                explosion ? List.of(FireworkExplosion.DEFAULT) : List.of()));
        return stack;
    }

    private static int rockets(FakePlayer player) {
        return player.level().getEntitiesOfClass(FireworkRocketEntity.class, player.getBoundingBox().inflate(4)).size();
    }
}
