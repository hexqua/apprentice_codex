package jp.aquafactory.apprenticecodex.gametest.storage.personalshelf;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelf;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlock;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.crafting.ComparatorScenarios.placeAndAssertBlockEntity;

final class PersonalShelfScenarios {
    private PersonalShelfScenarios() {}

    static void personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(0, 2, 0), "personal_shelf_vanilla_menu_test");
        var shelfPos = new BlockPos(0, 1, 0);
        placeAndAssertBlockEntity(helper, shelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(), BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
        var absoluteShelfPos = helper.absolutePos(shelfPos);
        var shelf = getPersonalShelfBlockEntity(helper, absoluteShelfPos);
        shelf.setShelfData(player, false, Direction.NORTH);
        shelf.setLifeRange(10.0);

        helper.runAtTickTime(1, () -> {
            var personalInventory = player.getCapability(Capabilities.PERSONAL_INVENTORY)
                    .orElseThrow(() -> new IllegalStateException("Missing personal inventory for Personal Shelf GameTest"));

            personalInventory.getHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
            for (var slot = 1; slot < 54; ++slot) {
                personalInventory.getHandler().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            player.getInventory().setItem(0, new ItemStack(Items.DIRT));

            helper.assertTrue(player.openMenu(shelf).isPresent(), "Personal Shelf should open a menu for its owner");
            helper.assertTrue(player.containerMenu instanceof ChestMenu chestMenu && chestMenu.getRowCount() == 6,
                    "Personal Shelf should expose a vanilla six-row chest menu");
            var chestMenu = (ChestMenu) player.containerMenu;
            helper.assertTrue(chestMenu.getSlot(0).getItem().is(Items.DIAMOND),
                    "Personal Shelf chest menu should read from the opener's personal inventory");

            var quickMoved = chestMenu.quickMoveStack(player, 81);
            helper.assertTrue(quickMoved.isEmpty(),
                    "Full Personal Shelf quick move should fail cleanly instead of looping");
            helper.assertTrue(player.getInventory().getItem(0).is(Items.DIRT),
                    "Failed Personal Shelf quick move should leave the player's stack in place");
            helper.succeed();
        });
    }

    static void personalShelfSynchronizesExportModeBlockState(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(1, 2, 1), "personal_shelf_export_state_test");
        var normalShelfPos = new BlockPos(0, 1, 0);
        var exportShelfPos = new BlockPos(2, 1, 0);

        placeAndAssertBlockEntity(helper, normalShelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(),
                BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());
        placeAndAssertBlockEntity(helper, exportShelfPos, BlockRegistry.PERSONAL_SHELF_CHEST.get(),
                BlockEntityRegistry.PERSONAL_SHELF_CHEST.get());

        var normalShelf = getPersonalShelfBlockEntity(helper, helper.absolutePos(normalShelfPos));
        normalShelf.setShelfData(player, false, Direction.NORTH);
        normalShelf.setLifeRange(10.0);

        var exportShelf = getPersonalShelfBlockEntity(helper, helper.absolutePos(exportShelfPos));
        exportShelf.setShelfData(player, true, Direction.NORTH);
        exportShelf.setLifeRange(10.0);

        helper.runAtTickTime(1, () -> {
            helper.assertBlockProperty(normalShelfPos, PersonalShelfChestBlock.EXPORT_MODE, false);
            helper.assertBlockProperty(exportShelfPos, PersonalShelfChestBlock.EXPORT_MODE, true);
            helper.succeed();
        });
    }

    static void personalShelfExpireClosesOpenedChestMenu(GameTestHelper helper) {
        var player = createPersonalShelfPlayer(helper, new BlockPos(0, 2, 0), "personal_shelf_expire_close_test");
        var shelfPos = new BlockPos(0, 1, 0);
        var absoluteShelfPos = castPersonalShelf(helper, player, shelfPos, false, Direction.NORTH);
        helper.runAtTickTime(1, () -> {
            var shelf = getPersonalShelfBlockEntity(helper, absoluteShelfPos);
            helper.assertTrue(player.openMenu(shelf).isPresent(), "Personal Shelf should open before the expiration check");
            helper.assertTrue(player.containerMenu instanceof ChestMenu,
                    "Personal Shelf should still be using ChestMenu during the expiration check");
            // 現在の Personal Shelf は tick 寿命ではなく、所有者が維持範囲外に出た時に失効する。
            var awayPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(10, 2, 0)));
            player.setPos(awayPos.x, awayPos.y, awayPos.z);
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(player.containerMenu == player.inventoryMenu,
                    "Expired Personal Shelf should close the opened menu");
            helper.assertBlockNotPresent(BlockRegistry.PERSONAL_SHELF_CHEST.get(), shelfPos);
        });
    }

    static FakePlayer createPersonalShelfPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        // owner lookup と openers の close 対象探索が server 側の player list / level lookup を使うため、
        // Personal Shelf の GameTest では FakePlayer もワールドへ参加させる。
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static BlockPos castPersonalShelf(GameTestHelper helper, FakePlayer player, BlockPos shelfPos, boolean exportMode, Direction exportFacing) {
        var spell = (PersonalShelf) SpellRegistry.PERSONAL_SHELF.get();
        var castData = new PersonalShelf.PersonalShelfCastData();
        var absoluteShelfPos = helper.absolutePos(shelfPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteShelfPos.getX());
        tag.putInt("PositionY", absoluteShelfPos.getY());
        tag.putInt("PositionZ", absoluteShelfPos.getZ());
        tag.putBoolean("ExportMode", exportMode);
        tag.putInt("ExportFacing", exportFacing.get3DDataValue());
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
        return absoluteShelfPos;
    }

    static PersonalShelfChestBlockEntity getPersonalShelfBlockEntity(GameTestHelper helper, BlockPos absoluteShelfPos) {
        var blockEntity = helper.getLevel().getBlockEntity(absoluteShelfPos);
        helper.assertTrue(blockEntity instanceof PersonalShelfChestBlockEntity,
                "Expected Personal Shelf block entity but found " + blockEntity);
        return (PersonalShelfChestBlockEntity) blockEntity;
    }
}
