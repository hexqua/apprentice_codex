package jp.aquafactory.apprenticecodex.gametest.enchantment.acquisition;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressEnchantingTableBonusHelper;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport.createPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantressTableGameTests {
    private EnchantressTableGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void equippedBonusRefreshesOnlyThirdOfferAndLoadedMenuClues(GameTestHelper helper)
            throws ReflectiveOperationException {
        try {
            verifyOffers(helper);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            ApprenticeCodex.LOGGER.error("Enchantress table GameTest failed", failure);
            throw failure;
        }
    }

    private static void verifyOffers(GameTestHelper helper) throws ReflectiveOperationException {
        var level = helper.getLevel();
        var table = helper.absolutePos(new BlockPos(2, 1, 2));
        var previousBlocks = new LinkedHashMap<BlockPos, BlockState>();
        try (var fixture = createPlayer(helper)) {
            var player = fixture.player();
            level.addNewPlayer(player);
            try {
                previousBlocks.put(table, level.getBlockState(table));
                level.setBlockAndUpdate(table, Blocks.ENCHANTING_TABLE.defaultBlockState());
                for (int x : new int[]{0, 4}) {
                    for (int z : new int[]{0, 4}) {
                        var gap = helper.absolutePos(new BlockPos((x + 2) / 2, 1, (z + 2) / 2));
                        previousBlocks.put(gap, level.getBlockState(gap));
                        level.setBlockAndUpdate(gap, Blocks.AIR.defaultBlockState());
                        var pos = helper.absolutePos(new BlockPos(x, 1, z));
                        previousBlocks.put(pos, level.getBlockState(pos));
                        level.setBlockAndUpdate(pos, Blocks.BOOKSHELF.defaultBlockState());
                    }
                }
                // 同一playerのseedを固定し、装備変更以外の候補・cost変化を除く。
                player.getRandom().setSeed(314159L);
                player.onEnchantmentPerformed(new ItemStack(Items.DIAMOND_SWORD), 0);
                var menu = createMenu(player, ContainerLevelAccess.create(level, table));
                player.containerMenu = menu;
                refresh(menu);
                var baseline = menu.costs.clone();
                long validShelves = EnchantingTableBlock.BOOKSHELF_OFFSETS.stream()
                        .filter(offset -> EnchantingTableBlock.isValidBookShelf(level, table, offset)).count();
                helper.assertTrue(validShelves >= 4, "The enchanting fixture must expose four valid bookshelves: " + validShelves);
                helper.assertTrue(menu.getSlot(0).getItem().isEnchantable(), "The fixture sword must be enchantable");
                helper.assertTrue(baseline[2] > 0, "The enchanting fixture must expose a third offer: "
                        + Arrays.toString(baseline));

                player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemRegistry.ENCHANTRESS_HAT.get()));
                refresh(menu);
                assertCosts(helper, menu, baseline, 0);
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.ENCHANTRESS_HAT.get()));
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemRegistry.ENCHANTRESS_ROBE.get()));
                int bonus = EnchantressEnchantingTableBonusHelper.getBonusForPlayer(player);
                helper.assertTrue(bonus > 0, "Equipped Enchantress armor must supply an enchanting bonus");
                for (int repeat = 0; repeat < 2; repeat++) {
                    refresh(menu);
                    assertCosts(helper, menu, baseline, bonus);
                    if (ModList.get().isLoaded("easymagic")) {
                        assertEasyMagicClues(helper, menu);
                    }
                }
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                refresh(menu);
                assertCosts(helper, menu, baseline, 0);
            } finally {
                player.containerMenu = player.inventoryMenu;
                level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                previousBlocks.forEach(level::setBlockAndUpdate);
            }
        }
        helper.succeed();
    }

    private static EnchantmentMenu createMenu(ServerPlayer player, ContainerLevelAccess access)
            throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("easymagic")) {
            return new EnchantmentMenu(0, player.getInventory(), access);
        }
        // 通常構成からoptional MODの型を読み込まず、導入構成では実際のメニューとmixinを通す。
        var type = Class.forName("fuzs.easymagic.world.inventory.ModEnchantmentMenu");
        return (EnchantmentMenu) type.getConstructor(int.class, Inventory.class, Container.class, ContainerLevelAccess.class)
                .newInstance(0, player.getInventory(), new SimpleContainer(3), access);
    }

    private static void refresh(EnchantmentMenu menu) {
        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.slotsChanged(menu.getSlot(0).container);
    }

    private static void assertCosts(GameTestHelper helper, EnchantmentMenu menu, int[] baseline, int bonus) {
        for (int row = 0; row < 3; row++) {
            int expected = baseline[row] + (row == 2 ? bonus : 0);
            helper.assertTrue(menu.costs[row] == expected,
                    "Enchantress bonus must refresh only the third offer without accumulating: row=" + row
                            + " expected=" + expected + " actual=" + menu.costs[row]);
        }
    }

    private static void assertEasyMagicClues(GameTestHelper helper, EnchantmentMenu menu)
            throws ReflectiveOperationException {
        var buildList = EnchantmentMenu.class.getDeclaredMethod("getEnchantmentList", RegistryAccess.class,
                ItemStack.class, int.class, int.class);
        buildList.setAccessible(true);
        var updated = buildList.invoke(menu, helper.getLevel().registryAccess(), menu.getSlot(0).getItem(), 2, menu.costs[2]);
        var createClue = menu.getClass().getDeclaredMethod("createClue", List.class);
        createClue.setAccessible(true);
        var expected = (List<?>) createClue.invoke(menu, updated);
        var clues = (List<?>) menu.getClass().getField("clues").get(menu);
        helper.assertTrue(describe((List<?>) clues.get(2)).equals(describe(expected)),
                "Easy Magic must rebuild its third clue from the bonus-adjusted offer");
    }

    private static List<String> describe(List<?> clues) {
        return clues.stream().map(value -> (EnchantmentInstance) value)
                .map(value -> value.enchantment.unwrapKey().orElseThrow().location() + ":" + value.level).toList();
    }
}
