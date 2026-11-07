package jp.aquafactory.apprenticecodex.gametest.spell.touchdig;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.invokeTouchDigDestroyBlock;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class TouchDigHarvestGameTests {
    private TouchDigHarvestGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scope = new MiningTestScope(helper);
            var spell = new TouchDigSpell();
            var playerPos = new BlockPos(4, 2, 4);

            var player = scope.track(createEquipmentTestPlayer(helper, playerPos, "touch_dig_range_test"));
            var magicData = MagicData.getPlayerMagicData(player);
            var targetPos = helper.absolutePos(new BlockPos(4, 13, 4));

            helper.assertTrue(magicData != null, "Touch Dig range test could not resolve player mana data");
            player.setYRot(0.0f);
            player.setXRot(-90.0f);
            player.setYHeadRot(0.0f);
            player.setYBodyRot(0.0f);
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should keep the default 8 block range without CraftsmansDelight");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should reach a target 12 blocks away when CraftsmansDelight is equipped"
                            + " [equipped=" + CraftsmansDelight.isEquippedBy(player)
                            + ", range=" + CraftsmansDelight.getTouchDigRange(player)
                            + "]");

            scope.run(() -> spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData));
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir(),
                    "Touch Dig should destroy the targeted block inside the extended range");
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigIgnoresCraftsmansDelightRingEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scope = new MiningTestScope(helper);
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(4, 2, 4);

            var player = scope.track(createEquipmentTestPlayer(helper, playerPos, "touch_dig_ignores_ring_enchant_test"));
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(silkTouch, 1);
            equipRingCurio(player, ringStack);

            var blockPos = helper.absolutePos(new BlockPos(4, 2, 5));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            var foreignPos = blockPos.getCenter();
            var foreignDrop = new ItemEntity(helper.getLevel(), foreignPos.x, foreignPos.y, foreignPos.z,
                    new ItemStack(Items.STONE));
            scope.onClose(foreignDrop::discard);
            helper.getLevel().addFreshEntity(foreignDrop);
            scope.run(() -> invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player));

            var drops = scope.entities(ItemEntity.class);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Touch Dig should use the unenchanted main-hand pickaxe and ignore ring Silk Touch");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Touch Dig should not inherit Silk Touch from CraftsmansDelight");
            helper.assertFalse(foreignDrop.isRemoved(), "Observing own drops must preserve pre-existing items");
        });
    }

    private static void equipRingCurio(ServerPlayer player, ItemStack ringStack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(Curios.RING_SLOT, 0, ringStack);
    }
}
