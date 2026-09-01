package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class StorageStabilizerGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final int SOURCE_SLOT = 0;

    private StorageStabilizerGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void inventoryRightClickOpensEnderChestAndConsumesMana(GameTestHelper helper) {
        var player = new MenuTrackingFakePlayer(helper, new BlockPos(0, 2, 0));
        var stabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
        StorageStabilizer.setSelectedSpellIndex(stabilizer, 1);
        player.getInventory().setItem(SOURCE_SLOT, stabilizer);
        player.getEnderChestInventory().setItem(0, new ItemStack(Items.DIAMOND));

        var enderChestSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SUMMON_ENDER_CHEST_SPELL.get();
        var magicData = MagicData.getPlayerMagicData(player);
        var manaCost = StorageStabilizer.getEnderChestManaCost();
        magicData.setMana(manaCost + 10.0F);
        magicData.getPlayerCooldowns().addCooldown(enderChestSpell, 100);

        var handled = stabilizer.getItem().overrideOtherStackedOnMe(
                stabilizer,
                ItemStack.EMPTY,
                new Slot(player.getInventory(), SOURCE_SLOT, 0, 0),
                ClickAction.SECONDARY,
                player,
                SlotAccess.NULL
        );

        helper.assertTrue(handled, "Storage Stabilizer inventory secondary click should be handled");
        helper.assertTrue(player.containerMenu instanceof ChestMenu chestMenu && chestMenu.getRowCount() == 3,
                "Storage Stabilizer inventory secondary click should open a vanilla three-row chest menu");
        helper.assertTrue(player.containerMenu.getSlot(0).getItem().is(Items.DIAMOND),
                "Storage Stabilizer menu should use the player's Ender Chest inventory");
        helper.assertTrue(magicData.getMana() == 10.0F,
                "Storage Stabilizer should consume the configured Summon Ender Chest mana cost");
        helper.assertFalse(magicData.isCasting(),
                "Storage Stabilizer inventory Ender Chest should not start a spell cast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(enderChestSpell),
                "Storage Stabilizer should ignore and preserve an existing Summon Ender Chest cooldown");
        helper.succeed();
    }

    private static final class MenuTrackingFakePlayer extends FakePlayer {
        private MenuTrackingFakePlayer(GameTestHelper helper, BlockPos pos) {
            super(helper.getLevel(), new GameProfile(
                    UUID.randomUUID(),
                    "storage_stabilizer_inventory_ender_chest_test"
            ));
            gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            var absolutePos = helper.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));
            setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        }

        @Override
        public OptionalInt openMenu(
                @Nullable MenuProvider menuProvider,
                @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter
        ) {
            if (menuProvider == null) {
                return OptionalInt.empty();
            }

            var menu = menuProvider.createMenu(1, getInventory(), this);
            if (menu == null) {
                return OptionalInt.empty();
            }

            containerMenu = menu;
            return OptionalInt.of(1);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void insufficientManaAndInvalidSlotsDoNotOpenEnderChest(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "storage_stabilizer_inventory_rejection_test"
        );
        var stabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
        player.getInventory().setItem(SOURCE_SLOT, stabilizer);
        var magicData = MagicData.getPlayerMagicData(player);
        var insufficientMana = Math.max(0.0F, StorageStabilizer.getEnderChestManaCost() - 1.0F);
        magicData.setMana(insufficientMana);

        StorageStabilizer.openEnderChestFromInventorySlot(player, SOURCE_SLOT);
        helper.assertTrue(player.containerMenu == player.inventoryMenu,
                "Storage Stabilizer should not open the Ender Chest when mana is insufficient");
        helper.assertTrue(magicData.getMana() == insufficientMana,
                "Rejected Storage Stabilizer use should not consume mana");

        magicData.setMana(StorageStabilizer.getEnderChestManaCost() + 10.0F);
        player.getInventory().setItem(SOURCE_SLOT, new ItemStack(Items.STICK));
        StorageStabilizer.openEnderChestFromInventorySlot(player, SOURCE_SLOT);
        StorageStabilizer.openEnderChestFromInventorySlot(player, -1);
        helper.assertTrue(player.containerMenu == player.inventoryMenu,
                "Invalid Storage Stabilizer inventory sources should not open the Ender Chest");
        helper.assertTrue(magicData.getMana() == StorageStabilizer.getEnderChestManaCost() + 10.0F,
                "Invalid Storage Stabilizer inventory sources should not consume mana");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tooltipShowsConfiguredManaCostInAqua(GameTestHelper helper) {
        var stabilizer = new ItemStack(ItemRegistry.STORAGE_STABILIZER.get());
        ApprenticeCodexGameTestScenarios.assertTooltipKeyAt(
                helper,
                stabilizer,
                1,
                "item.apprenticecodex.storage_stabilizer.ender_help",
                "Storage Stabilizer should show its inventory Ender Chest help"
        );
        ApprenticeCodexGameTestScenarios.assertTooltipKeyUsesColor(
                helper,
                stabilizer,
                "item.apprenticecodex.storage_stabilizer.ender_help",
                ChatFormatting.GRAY,
                "Storage Stabilizer Ender Chest help should use gray body text"
        );
        ApprenticeCodexGameTestScenarios.assertTooltipKeyArgumentUsesColor(
                helper,
                stabilizer,
                "item.apprenticecodex.storage_stabilizer.ender_help",
                0,
                TextColor.fromLegacyFormat(ChatFormatting.AQUA),
                "Storage Stabilizer Ender Chest mana cost should use aqua"
        );
        helper.succeed();
    }
}
