package jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateProcessingTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.hasItemEntityWithin;
import static jp.aquafactory.apprenticecodex.gametest.support.DamageTestSupport.setMaxHealthForDamageTest;
import static jp.aquafactory.apprenticecodex.gametest.support.EntityTestSupport.spawnNoGravityItem;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.RegistryTestSupport.requireForgeItem;

final class HeavenlyFistScenarios {
    private HeavenlyFistScenarios() {}

    static void heavenlyFistCreatePressingDenylistLeavesDepotItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var deniedRecipeId = ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "pressing/sugar_cane");
        var override = ApprenticeCodexServerConfig.useProcessingRecipeDenylistOverrideForGameTest(
                List.of(),
                List.of(),
                List.of(),
                List.of(deniedRecipeId.toString()),
                List.of()
        );
        invokeCreateGameTestHookVoid(
                "placeDepotWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.SUGAR_CANE)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            try {
                var result = invokeCreateGameTestHookItemStack("getDepotItem", level, helper.absolutePos(targetPos));
                helper.assertTrue(result.is(Items.SUGAR_CANE),
                        "Heavenly Fist should leave denied Create pressing inputs untouched: " + result);
                helper.succeed();
            } finally {
                override.close();
            }
        });
    }

    static void heavenlyFistImpactAabbAppliesDamageAndGravityBound(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_owner_test");
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        target.setNoAi(true);
        setMaxHealthForDamageTest(target, 100.0F);

        var center = helper.absolutePos(new BlockPos(2, 2, 0)).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 8.0F, 2.0F, 0);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            helper.assertTrue(target.getHealth() < 100.0F,
                    "Heavenly Fist should damage CombatTarget inside its locked AABB");
            helper.assertTrue(target.hasEffect(EffectRegistry.GRAVITY_BOUND.get()),
                    "Heavenly Fist should apply Gravity Bound to damaged CombatTarget");
            helper.succeed();
        });
    }

    static void heavenlyFistImpactDamagesNonLivingCombatTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_crystal_owner_test");
        var crystal = helper.spawn(EntityType.END_CRYSTAL, new BlockPos(2, 2, 0));

        var center = helper.absolutePos(new BlockPos(2, 2, 0)).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 8.0F, 2.0F, 0);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            helper.assertTrue(crystal.isRemoved() || !crystal.isAlive(),
                    "Heavenly Fist should damage non-Living CombatTarget inside its locked AABB");
            helper.succeed();
        });
    }

    static void heavenlyFistImpactDoesNotTrackMovedTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_lock_owner_test");
        // 追尾の有無だけを測り、近隣テストのField Overseerが選ぶ敵対mobをfixtureに使わない。
        var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 0));
        target.setNoAi(true);
        target.setNoGravity(true);
        setMaxHealthForDamageTest(target, 100.0F);

        var center = helper.absolutePos(new BlockPos(2, 2, 0)).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 8.0F, 1.0F, 0);
        level.addFreshEntity(fist);

        helper.runAtTickTime(5, () -> target.moveTo(helper.absolutePos(new BlockPos(7, 2, 0)).getCenter()));
        helper.runAtTickTime(28, () -> {
            helper.assertTrue(Math.abs(target.getHealth() - 100.0F) < 0.01F,
                    "Heavenly Fist should not chase a target after locking the impact position: health="
                            + target.getHealth() + ", position=" + target.position()
                            + ", lastDamage=" + target.getLastDamageSource() + ", inWall=" + target.isInWall()
                            + ", fistPosition=" + fist.position() + ", fistTick=" + fist.tickCount);
            helper.assertFalse(target.hasEffect(EffectRegistry.GRAVITY_BOUND.get()),
                    "Heavenly Fist should not apply Gravity Bound to a moved-out target");
            helper.succeed();
        });
    }

    static void heavenlyFistProcessesCreateDepotItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_create_depot_owner_test");
        invokeCreateGameTestHookVoid(
                "placeDepotWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.SUGAR_CANE)
        );

        var center = helper.absolutePos(targetPos).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, 2.0F, 1);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            var result = invokeCreateGameTestHookItemStack("getDepotItem", level, helper.absolutePos(targetPos));
            helper.assertTrue(result.is(Items.PAPER), "Heavenly Fist should process Create Depot items: " + result);
            helper.succeed();
        });
    }

    static void heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var farItemPos = targetPos.offset(3, 0, 0);
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_create_drop_area_owner_test");
        spawnNoGravityItem(helper, farItemPos, new ItemStack(Items.SUGAR_CANE));

        var center = helper.absolutePos(targetPos).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, 4.0F, 1);
        level.addFreshEntity(fist);

        helper.runAtTickTime(28, () -> {
            var farItemCenter = helper.absoluteVec(Vec3.atCenterOf(farItemPos));
            helper.assertTrue(hasItemEntityWithin(level, Items.SUGAR_CANE, farItemCenter, 0.75D),
                    "Heavenly Fist should leave dropped Create inputs outside its 3x2x3 process area");
            helper.assertFalse(hasItemEntityWithin(level, Items.PAPER, farItemCenter, 0.75D),
                    "Heavenly Fist should not process dropped Create items outside its 3x2x3 process area");
            helper.succeed();
        });
    }

    static void heavenlyFistProcessesCreateBasinCompacting(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var cinderFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "cinder_flour"));
        var blazeCakeBase = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "blaze_cake_base"));
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.EGG),
                new ItemStack(Items.SUGAR),
                new ItemStack(cinderFlour)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            var resultCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(blazeCakeBase));
            helper.assertTrue(resultCount == 1, "Heavenly Fist should process Create Basin compacting once: " + resultCount);
            helper.succeed();
        });
    }

    static void heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var cinderFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "cinder_flour"));
        var blazeCakeBase = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "blaze_cake_base"));
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.EGG, 2),
                new ItemStack(Items.SUGAR, 2),
                new ItemStack(cinderFlour, 2)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            var resultCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(blazeCakeBase));
            var remainingCinderFlourCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(cinderFlour));
            helper.assertTrue(resultCount == 1, "Heavenly Fist should spend one budget per Basin recipe: " + resultCount);
            helper.assertTrue(remainingCinderFlourCount == 1,
                    "Heavenly Fist should leave the second compacting input set when budget is one: " + remainingCinderFlourCount);
            helper.succeed();
        });
    }

    static void heavenlyFistCreateCompactingDenylistLeavesBasinItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var deniedRecipeId = ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "compacting/blaze_cake");
        var cinderFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "cinder_flour"));
        var blazeCakeBase = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "blaze_cake_base"));
        var override = ApprenticeCodexServerConfig.useProcessingRecipeDenylistOverrideForGameTest(
                List.of(),
                List.of(),
                List.of(),
                List.of(deniedRecipeId.toString()),
                List.of()
        );
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(Items.EGG),
                new ItemStack(Items.SUGAR),
                new ItemStack(cinderFlour)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            try {
                var resultCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(blazeCakeBase));
                var remainingCinderFlourCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(cinderFlour));
                helper.assertTrue(resultCount == 0,
                        "Heavenly Fist should not process denied Create Basin compacting recipes: " + resultCount);
                helper.assertTrue(remainingCinderFlourCount == 1,
                        "Heavenly Fist should leave denied compacting inputs untouched: " + remainingCinderFlourCount);
                helper.succeed();
            } finally {
                override.close();
            }
        });
    }

    static void heavenlyFistSkipsCreateBasinCompressionCrafting(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var targetPos = new BlockPos(2, 1, 0);
        var zincIngot = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "zinc_ingot"));
        var zincBlock = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "zinc_block"));
        placeCreateBasinWithItems(
                level,
                helper.absolutePos(targetPos),
                new ItemStack(zincIngot, 9)
        );

        spawnHeavenlyFistForCreateProcess(helper, targetPos, 1);

        helper.runAtTickTime(28, () -> {
            var zincBlockCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(zincBlock));
            var zincIngotCount = getCreateBasinItemCount(level, helper.absolutePos(targetPos), new ItemStack(zincIngot));
            helper.assertTrue(zincBlockCount == 0, "Heavenly Fist should not run Create Basin compression crafting: " + zincBlockCount);
            helper.assertTrue(zincIngotCount == 9, "Heavenly Fist should leave compression crafting inputs untouched: " + zincIngotCount);
            helper.succeed();
        });
    }

    static void spawnHeavenlyFistForCreateProcess(GameTestHelper helper, BlockPos targetPos, int maxProcessCount) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "heavenly_fist_create_process_owner_test");
        var center = helper.absolutePos(targetPos).getCenter();
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, 2.0F, maxProcessCount);
        level.addFreshEntity(fist);
    }

    static void gravityBoundPullsAirborneTargetsDown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var phantom = helper.spawn(EntityType.PHANTOM, new BlockPos(0, 6, 0));
            phantom.setNoAi(true);
            phantom.setDeltaMovement(0.0D, 0.6D, 0.0D);

            EffectRegistry.GRAVITY_BOUND.get().applyEffectTick(phantom, 9);

            helper.assertTrue(phantom.getDeltaMovement().y <= -1.25D,
                    "Gravity Bound should force airborne targets downward regardless of amplifier: "
                            + phantom.getDeltaMovement().y);
        });
    }
}
