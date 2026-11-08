package jp.aquafactory.apprenticecodex.gametest.storage.accessorycase;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCase;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.NotNull;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AccessoryCaseInventoryClickGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final int SOURCE_SLOT = 0;

    private AccessoryCaseInventoryClickGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void inventoryClickPacketPreservesSlotSynchronizationAfterClosing(GameTestHelper helper) {
        var fixture = new ClickFixture(helper);
        try {
            var player = fixture.player;
            var caseStack = player.getInventory().getItem(SOURCE_SLOT);
            new SpellcasterAccessoryCase.CaseInventory(caseStack, player)
                    .insertItem(0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()), false);
            player.initInventoryMenu();
            fixture.rightClickCase();
            helper.runAfterDelay(2, () -> {
                try (fixture) {
                    helper.assertTrue(player.containerMenu instanceof SpellcasterAccessoryCaseMenu,
                            "Inventory click packet should open the accessory case");
                    helper.assertTrue(player.containerMenu.getSlot(0).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                            "Opened accessory case should expose the clicked case's stored ring");
                    player.doCloseContainer();
                    helper.assertTrue(player.containerMenu == player.inventoryMenu,
                            "Closing the accessory case should restore the original inventory menu");
                    fixture.packets.clear();
                    player.getInventory().setItem(9, new ItemStack(Items.DIAMOND));
                    player.inventoryMenu.broadcastChanges();
                    var targetSlot = player.inventoryMenu.findSlot(player.getInventory(), 9).orElseThrow();
                    helper.assertTrue(fixture.packets.stream().anyMatch(packet ->
                                    packet instanceof ClientboundContainerSetSlotPacket update
                                            && update.getContainerId() == player.inventoryMenu.containerId
                                            && update.getSlot() == targetSlot && update.getItem().is(Items.DIAMOND)),
                            "Inventory changes after closing the accessory case must still send slot update packets");
                    helper.succeed();
                }
            });
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        }
    }

    @GameTest(template = TEMPLATE)
    public static void deferredOpenRejectsReplacedSource(GameTestHelper helper) {
        // 同種の別ケースへの置換も拒否し、クリックした収納物との同一性を守る。
        assertDeferredOpenRejected(helper, player -> player.getInventory().setItem(
                SOURCE_SLOT, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get())
        ), "Replacing the clicked case must cancel deferred opening");
    }

    @GameTest(template = TEMPLATE)
    public static void deferredOpenRejectsOccupiedCursor(GameTestHelper helper) {
        assertDeferredOpenRejected(helper,
                player -> player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND)),
                "Picking up a cursor item must cancel deferred opening");
    }

    @GameTest(template = TEMPLATE)
    public static void deferredOpenPreservesChangedMenu(GameTestHelper helper) {
        assertDeferredOpenRejected(helper,
                player -> player.containerMenu = ChestMenu.threeRows(1, player.getInventory()),
                "Changing menus must cancel deferred opening without replacing the new menu");
    }

    private static void assertDeferredOpenRejected(
            GameTestHelper helper, Consumer<ServerPlayer> changeBeforeOpening, String message
    ) {
        var fixture = new ClickFixture(helper);
        try {
            fixture.player.initInventoryMenu();
            fixture.rightClickCase();
            helper.assertTrue(fixture.player.containerMenu == fixture.player.inventoryMenu,
                    "Accessory case opening should wait until the inventory click completes");
            changeBeforeOpening.accept(fixture.player);
            var expectedMenu = fixture.player.containerMenu;
            helper.runAfterDelay(2, () -> {
                try (fixture) {
                    helper.assertTrue(fixture.player.containerMenu == expectedMenu, message);
                    helper.succeed();
                }
            });
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        }
    }

    private static final class ClickFixture implements AutoCloseable {
        private final ServerPlayer player;
        private final EmbeddedChannel channel;
        private final ArrayList<Packet<?>> packets = new ArrayList<>();

        private ClickFixture(GameTestHelper helper) {
            var profile = new GameProfile(UUID.randomUUID(), "accessory_case_sync_test");
            var cookie = CommonListenerCookie.createInitial(profile, false);
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            try {
                NetworkRegistry.configureMockConnection(connection);
                new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
                    @Override
                    public void send(@NotNull Packet<?> packet) {
                        packets.add(packet);
                    }
                };
                player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
                player.getInventory().setItem(SOURCE_SLOT, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }

        private void rightClickCase() {
            // hook直呼びでは検出できない、vanillaの同期停止・再開を含む経路を通す。
            var menu = player.inventoryMenu;
            var sourceMenuSlot = menu.findSlot(player.getInventory(), SOURCE_SLOT).orElseThrow();
            player.connection.handleContainerClick(new ServerboundContainerClickPacket(
                    menu.containerId, menu.getStateId(), sourceMenuSlot, 1, ClickType.PICKUP,
                    ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()
            ));
        }

        @Override
        public void close() {
            channel.finishAndReleaseAll();
        }
    }
}
