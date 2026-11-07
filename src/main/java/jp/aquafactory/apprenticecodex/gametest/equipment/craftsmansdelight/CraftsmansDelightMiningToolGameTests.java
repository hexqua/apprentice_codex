package jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight;

import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjack;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CraftsmansDelightMiningToolGameTests {
    private CraftsmansDelightMiningToolGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void craftsmansDelightDummyToolsCopyAllCurrentMainHandEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scope = new MiningTestScope(helper);
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var sharpness = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
            var unbreaking = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var player = scope.track(createEquipmentTestPlayer(helper, new BlockPos(4, 2, 4), "craftsmans_main_hand_enchant_copy_test"));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var heldSword = new ItemStack(Items.DIAMOND_SWORD);
            heldSword.enchant(sharpness, 4);
            heldSword.enchant(unbreaking, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldSword);

            var lumberjackTool = TinyLumberjack.createDummyTool(player);
            var worldFlatterTool = WorldFlatter.createDummyTool(player);
            var heavenlyFistTool = CraftsmansDelight.createHeavenlyFistCrystalHarvestTool(player);
            helper.assertTrue(lumberjackTool.is(Items.IRON_AXE),
                    "Tiny Lumberjack should keep its iron axe dummy tool");
            helper.assertTrue(worldFlatterTool.is(Items.NETHERITE_PICKAXE),
                    "World Flatter should keep its CraftsmansDelight netherite pickaxe dummy tool");
            helper.assertTrue(heavenlyFistTool.is(Items.DIAMOND_PICKAXE),
                    "Heavenly Fist should keep its diamond pickaxe dummy tool");
            for (var dummyTool : List.of(lumberjackTool, worldFlatterTool, heavenlyFistTool)) {
                helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                                dummyTool,
                                net.minecraft.world.item.enchantment.Enchantments.SHARPNESS
                        ) == 4,
                        "CraftsmansDelight should copy even non-mining main-hand enchantments without filtering");
                helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                                dummyTool,
                                net.minecraft.world.item.enchantment.Enchantments.UNBREAKING
                        ) == 3,
                        "CraftsmansDelight should copy every main-hand enchantment");
            }

            var heldPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            heldPickaxe.enchant(silkTouch, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldPickaxe);
            var updatedLumberjackTool = TinyLumberjack.createDummyTool(player);
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            updatedLumberjackTool,
                            net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH
                    ) == 1,
                    "CraftsmansDelight should follow the current main-hand enchantments at destruction time");
            helper.assertTrue(jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                            updatedLumberjackTool,
                            net.minecraft.world.item.enchantment.Enchantments.SHARPNESS
                    ) == 0,
                    "CraftsmansDelight should not retain enchantments from the previous main-hand item");
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void spectralHammerCopiesCurrentMainHandWithCraftsmansDelight(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scope = new MiningTestScope(helper);
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(4, 2, 4);

            var player = scope.track(createEquipmentTestPlayer(helper, playerPos, "spectral_hammer_main_hand_copy_test"));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var unenchantedTool = new ItemStack(Items.IRON_PICKAXE);
            unenchantedTool.setDamageValue(7);
            player.setItemInHand(InteractionHand.MAIN_HAND, unenchantedTool);
            var copiedTool = CraftsmansDelight.createSpectralHammerTool(player);
            helper.assertTrue(copiedTool.is(Items.IRON_PICKAXE) && copiedTool.getDamageValue() == 7,
                    "Spectral Hammer should copy an unenchanted main-hand tool while CraftsmansDelight is equipped");
            copiedTool.setDamageValue(8);
            helper.assertTrue(unenchantedTool.getDamageValue() == 7,
                    "Spectral Hammer should return an independent copy of the main-hand tool");

            var silkTool = new ItemStack(Items.DIAMOND_PICKAXE);
            silkTool.enchant(silkTouch, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, silkTool);

            var targetPos = helper.absolutePos(new BlockPos(4, 2, 6));
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            var hammer = new SpectralHammer(
                    helper.getLevel(),
                    player,
                    new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.NORTH, targetPos, false),
                    0,
                    1
            );
            var hammerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(4, 2, 5)));
            hammer.setPos(hammerPos.x, hammerPos.y, hammerPos.z);
            scope.run(() -> helper.getLevel().addFreshEntity(hammer));

            for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                scope.run(hammer::tick);
            }

            var drops = scope.entities(ItemEntity.class);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Spectral Hammer should apply main-hand Silk Touch while CraftsmansDelight is equipped");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Spectral Hammer with main-hand Silk Touch should not drop cobblestone");
        });
    }

    private static void equipRingCurio(ServerPlayer player, ItemStack ringStack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(Curios.RING_SLOT, 0, ringStack);
    }
}
