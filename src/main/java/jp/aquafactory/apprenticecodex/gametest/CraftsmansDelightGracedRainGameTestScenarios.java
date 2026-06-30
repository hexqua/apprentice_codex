package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRainCloudEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.lang.reflect.Field;
import java.util.List;

final class CraftsmansDelightGracedRainGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final int STARTING_AGE = -1000;
    private static final int STARTING_BREEDING_COOLDOWN = 1000;
    private static final int EXPECTED_REDUCED_AGE = -900;
    private static final int EXPECTED_REDUCED_BREEDING_COOLDOWN = 900;
    private static final int DUPLICATION_COOLDOWN = 1000;

    private CraftsmansDelightGracedRainGameTestScenarios() {
    }

    static void craftsmansDelightGracedRainAcceleratesBabyGrowth(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createGracedRainOwner(helper, "graced_rain_baby_growth_owner", true);
            var cow = spawnAgeableTarget(helper, EntityType.COW, STARTING_AGE);

            processGracedRainEntityEffect(helper, owner, cow);

            helper.assertTrue(cow.getAge() == EXPECTED_REDUCED_AGE,
                    "Craftsman's Delight Graced Rain should reduce remaining baby growth age from "
                            + STARTING_AGE + " to " + EXPECTED_REDUCED_AGE + " but got " + cow.getAge());
        });
    }

    static void craftsmansDelightGracedRainReducesBreedingCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createGracedRainOwner(helper, "graced_rain_breeding_cooldown_owner", true);
            var cow = spawnAgeableTarget(helper, EntityType.COW, STARTING_BREEDING_COOLDOWN);

            processGracedRainEntityEffect(helper, owner, cow);

            helper.assertTrue(cow.getAge() == EXPECTED_REDUCED_BREEDING_COOLDOWN,
                    "Craftsman's Delight Graced Rain should reduce breeding cooldown from "
                            + STARTING_BREEDING_COOLDOWN + " to " + EXPECTED_REDUCED_BREEDING_COOLDOWN
                            + " but got " + cow.getAge());
        });
    }

    static void gracedRainWithoutCraftsmansDelightLeavesMobAgeUnchanged(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createGracedRainOwner(helper, "graced_rain_no_ring_owner", false);
            var cow = spawnAgeableTarget(helper, EntityType.COW, STARTING_AGE);

            processGracedRainEntityEffect(helper, owner, cow);

            helper.assertTrue(cow.getAge() == STARTING_AGE,
                    "Graced Rain without Craftsman's Delight should not change age but got " + cow.getAge());
        });
    }

    static void gracedRainUndeadTargetsKeepDamageBehavior(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createGracedRainOwner(helper, "graced_rain_undead_owner", true);
            var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            zombie.setNoAi(true);
            var startingHealth = zombie.getHealth();

            processGracedRainEntityEffect(helper, owner, zombie);

            helper.assertTrue(zombie.getHealth() < startingHealth,
                    "Graced Rain should keep damaging undead targets while Craftsman's Delight is equipped");
        });
    }

    static void craftsmansDelightGracedRainGrowthDenylistBlocksOnlyGrowth(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCraftsmansDelightGracedRainDenylistOverrideForGameTest(
                    List.of(cowId()),
                    List.of()
            )) {
                var owner = createGracedRainOwner(helper, "graced_rain_growth_denylist_owner", true);
                var babyCow = spawnAgeableTarget(helper, EntityType.COW, STARTING_AGE, new BlockPos(0, 2, 0));
                var cooldownCow = spawnAgeableTarget(helper, EntityType.COW, STARTING_BREEDING_COOLDOWN, new BlockPos(2, 2, 0));

                processGracedRainEntityEffect(helper, owner, babyCow);
                processGracedRainEntityEffect(helper, owner, cooldownCow);

                helper.assertTrue(babyCow.getAge() == STARTING_AGE,
                        "Graced Rain growth denylist should leave baby age unchanged but got " + babyCow.getAge());
                helper.assertTrue(cooldownCow.getAge() == EXPECTED_REDUCED_BREEDING_COOLDOWN,
                        "Graced Rain growth denylist should not block breeding cooldown reduction but got "
                                + cooldownCow.getAge());
            }
        });
    }

    static void craftsmansDelightGracedRainBreedingCooldownDenylistBlocksOnlyCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useCraftsmansDelightGracedRainDenylistOverrideForGameTest(
                    List.of(),
                    List.of(cowId())
            )) {
                var owner = createGracedRainOwner(helper, "graced_rain_breeding_denylist_owner", true);
                var babyCow = spawnAgeableTarget(helper, EntityType.COW, STARTING_AGE, new BlockPos(0, 2, 0));
                var cooldownCow = spawnAgeableTarget(helper, EntityType.COW, STARTING_BREEDING_COOLDOWN, new BlockPos(2, 2, 0));

                processGracedRainEntityEffect(helper, owner, babyCow);
                processGracedRainEntityEffect(helper, owner, cooldownCow);

                helper.assertTrue(babyCow.getAge() == EXPECTED_REDUCED_AGE,
                        "Graced Rain breeding cooldown denylist should not block baby growth but got "
                                + babyCow.getAge());
                helper.assertTrue(cooldownCow.getAge() == STARTING_BREEDING_COOLDOWN,
                        "Graced Rain breeding cooldown denylist should leave cooldown unchanged but got "
                                + cooldownCow.getAge());
            }
        });
    }

    static void craftsmansDelightGracedRainDoesNotReduceAllayDuplicationCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createGracedRainOwner(helper, "graced_rain_allay_owner", true);
            var allay = helper.spawn(EntityType.ALLAY, new BlockPos(0, 2, 0));
            allay.setNoAi(true);
            setAllayDuplicationCooldown(allay, DUPLICATION_COOLDOWN);

            processGracedRainEntityEffect(helper, owner, allay);

            var actualCooldown = getAllayDuplicationCooldown(allay);
            helper.assertTrue(actualCooldown == DUPLICATION_COOLDOWN,
                    "Craftsman's Delight Graced Rain should not reduce Allay duplication cooldown but got "
                            + actualCooldown);
        });
    }

    private static FakePlayer createGracedRainOwner(GameTestHelper helper, String profileName, boolean equipCraftsmansDelight) {
        var owner = createTrackedEquipmentTestPlayer(helper, new BlockPos(4, 2, 0), profileName);
        if (equipCraftsmansDelight) {
            equipRingCurio(owner, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
        }
        return owner;
    }

    private static <T extends AgeableMob> T spawnAgeableTarget(GameTestHelper helper, EntityType<T> entityType, int age) {
        return spawnAgeableTarget(helper, entityType, age, new BlockPos(0, 2, 0));
    }

    private static <T extends AgeableMob> T spawnAgeableTarget(
            GameTestHelper helper,
            EntityType<T> entityType,
            int age,
            BlockPos position
    ) {
        var target = helper.spawn(entityType, position);
        target.setNoAi(true);
        target.setAge(age);
        return target;
    }

    private static void processGracedRainEntityEffect(GameTestHelper helper, FakePlayer owner, LivingEntity target) {
        var level = helper.getLevel();
        var cloud = new GracedRainCloudEntity(EntityRegistry.GRACED_RAIN_CLOUD.get(), level, owner);
        cloud.setEffectRadiusBlocks(1);
        cloud.setHealAmount(2.0F);
        cloud.setAnchorPosition(target.position().add(0.0D, GracedRainCloudEntity.HEIGHT_OFFSET, 0.0D));
        for (var tick = 0; tick < 10; ++tick) {
            cloud.tickOnServer(level);
        }
    }

    private static String cowId() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW).toString();
    }

    private static void setAllayDuplicationCooldown(Allay allay, long cooldown) {
        try {
            var field = allayDuplicationCooldownField();
            field.setLong(allay, cooldown);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set Allay duplication cooldown for GameTest", exception);
        }
    }

    private static long getAllayDuplicationCooldown(Allay allay) {
        try {
            var field = allayDuplicationCooldownField();
            return field.getLong(allay);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not read Allay duplication cooldown for GameTest", exception);
        }
    }

    private static Field allayDuplicationCooldownField() throws NoSuchFieldException {
        var field = Allay.class.getDeclaredField("duplicationCooldown");
        field.setAccessible(true);
        return field;
    }
}
