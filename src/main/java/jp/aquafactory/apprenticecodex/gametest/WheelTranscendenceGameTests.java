package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.entity.broom.BroomSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.item.broom.BroomCurioSupport;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AnvilMenu;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class WheelTranscendenceGameTests {
    private WheelTranscendenceGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void targetsAcceptBooksAtAnvilOnly(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "wheel_enchant_anvil");
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE);
        for (var item : List.of(ItemRegistry.ARCHIVISTS_GRIMOIRE.get(), ItemRegistry.FLOATMOUNT_BROOM.get(),
                ItemRegistry.HOVERRIDE_BROOM.get(), ItemRegistry.SHOOTING_STAR_MANTLE.get())) {
            var stack = new ItemStack(item);
            var book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(enchantment, 1);
            var menu = new AnvilMenu(0, player.getInventory());
            menu.getSlot(0).set(stack);
            menu.getSlot(1).set(book);
            menu.createResult();
            var result = menu.getSlot(2).getItem();
            helper.assertTrue(!result.isEmpty() && result.getEnchantmentLevel(enchantment) == 1,
                    "Anvil must apply Transcendence to " + item);
            helper.assertTrue(item.getEnchantmentValue(stack) == 0,
                    "Direct enchanting-table eligibility must remain unchanged");
        }
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void grimoireResolvesWheelAndPreservesScrolls(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(0, 2, 0), "wheel_transcendence");
        MagicData.getPlayerMagicData(player).setMana(10000);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE);
        var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
        var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
        AffinityData.setAffinityData(ring, spell, 2);
        curios.setEquippedCurio("head", 0, ring);
        try {
            for (int level : new int[]{0, 1, 3, 10}) {
                var stack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
                if (level > 0) stack.enchant(enchantment, level);
                var inventory = new ArchivistsGrimoire.ScrollInventory(stack, player.registryAccess());
                var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spell, spell.getMaxLevel(), scroll);
                inventory.setStackInSlot(1, scroll);
                inventory.setStackInSlot(4, scroll.copy());
                curios.setEquippedCurio(Curios.SPELLBOOK_SLOT, 0, stack);
                var snapshot = stack.copy();
                int expected = spell.getMaxLevel() + (level > 0 ? 1 : 0);
                for (int repeat = 0; repeat < 2; repeat++) {
                    var options = new SpellSelectionManager(player).getSpellsForSlot(Curios.SPELLBOOK_SLOT);
                    helper.assertTrue(options.size() == 2 && options.get(0).slotIndex == 1
                                    && options.get(1).slotIndex == 4,
                            "Duplicate spells and sparse grimoire slots must remain independent");
                    helper.assertTrue(options.stream().allMatch(option -> option.spellData.getLevel() == expected),
                            "Grimoire wheel must apply the fixed bonus exactly once");
                    helper.assertTrue(ArchivistsGrimoire.getResolvedVisibleSpell(stack, 1, player.registryAccess())
                                    .getLevel() == expected,
                            "Grimoire tooltip data must agree with its wheel");
                    helper.assertTrue(spell.getLevelFor(options.getFirst().spellData.getLevel(), player) == expected + 2,
                            "Affinity must stack once after the internal bonus");
                }
                helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(stack, 1, player.registryAccess()).getLevel()
                                == spell.getMaxLevel() && ItemStack.isSameItemSameComponents(snapshot, stack),
                        "Resolution must not change stored scrolls or legacy enchantments");
                helper.assertFalse(ISpellContainer.isSpellContainer(stack), "Grimoire must not gain a container");
                // 実際のホイール入力が解決した値を詠唱開始イベントで観測し、効果生成は止める。
                int[] observed = {-1};
                Consumer<SpellPreCastEvent> listener = event -> {
                    if (event.getEntity() == player) {
                        observed[0] = event.getSpellLevel();
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(listener);
                try {
                    var option = new SpellSelectionManager(player).getSpellsForSlot(Curios.SPELLBOOK_SLOT).getFirst();
                    Utils.serverSideInitiateQuickCast(player, option.globalIndex);
                    helper.assertTrue(observed[0] == expected + 2,
                            "Wheel cast initiation must receive Transcendence and Affinity exactly once");
                } finally {
                    NeoForge.EVENT_BUS.unregister(listener);
                }
                var extracted = inventory.extractItem(1, 1, false);
                helper.assertTrue(ISpellContainer.get(extracted).getSpellAtIndex(0).getLevel() == spell.getMaxLevel(),
                        "Extracted grimoire scroll must keep its original level");
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(template = "gametest/basic_floor")
    public static void broomsResolveOnlyMountedScrolls(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.TRANSCENDENCE);
        for (var item : List.of(ItemRegistry.FLOATMOUNT_BROOM.get(), ItemRegistry.HOVERRIDE_BROOM.get())) {
            var player = createRider(helper);
            var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            AffinityData.setAffinityData(ring, spell, 2);
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("head", 0, ring);
            var broom = item == ItemRegistry.FLOATMOUNT_BROOM.get()
                    ? EntityRegistry.FLOATMOUNT_BROOM.get().create(helper.getLevel())
                    : EntityRegistry.HOVERRIDE_BROOM.get().create(helper.getLevel());
            try {
                helper.assertTrue(broom != null, "Broom entity must exist");
                if (broom == null) throw new IllegalStateException("Missing broom entity");
                broom.setPos(player.position());
                helper.getLevel().addFreshEntity(broom);
                helper.assertTrue(player.startRiding(broom, true), "Test rider must mount");
                for (int level : new int[]{0, 1, 3, 10}) {
                    var stack = new ItemStack(item);
                    if (level > 0) stack.enchant(enchantment, level);
                    SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()));
                    helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 1,
                            new ItemStack(Items.SADDLE)), "CallBroom test requires back-slot calibration");
                    var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                    ISpellContainer.createScrollContainer(spell, spell.getMaxLevel(), scroll);
                    AbstractBroomItem.setCalibrationScroll(stack, 0, scroll);
                    AbstractBroomItem.setCalibrationScroll(stack, 2, scroll.copy());
                    broom.setBroomItemStack(stack);
                    CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(BroomCurioSupport.CURIO_SLOT, 0, stack);
                    for (int repeat = 0; repeat < 2; repeat++) {
                        var manager = new SpellSelectionManager(player);
                        var options = manager.getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT);
                        helper.assertTrue(options.size() == 1 && options.getFirst().slotIndex == 0
                                        && options.getFirst().spellData.getLevel() == spell.getMaxLevel() + (level > 0 ? 1 : 0),
                                "Only enabled mounted broom scrolls must receive the fixed bonus");
                        var call = manager.getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT);
                        helper.assertTrue(call.size() == 1 && call.getFirst().spellData.getLevel() == 1,
                                "Built-in CallBroom must remain level one");
                    }
                    int[] observed = {-1};
                    Consumer<SpellPreCastEvent> listener = event -> {
                        if (event.getEntity() == player) {
                            observed[0] = event.getSpellLevel();
                            event.setCanceled(true);
                        }
                    };
                    NeoForge.EVENT_BUS.addListener(listener);
                    try {
                        var option = new SpellSelectionManager(player)
                                .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).getFirst();
                        Utils.serverSideInitiateQuickCast(player, option.globalIndex);
                        helper.assertTrue(observed[0] == spell.getMaxLevel() + (level > 0 ? 1 : 0) + 2,
                                "Mounted broom casting must apply Transcendence and Affinity once");
                    } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                    }
                    var restored = ItemStack.parse(player.registryAccess(), broom.getBroomItemStack()
                            .saveOptional(player.registryAccess())).orElseThrow();
                    helper.assertTrue(ItemStack.isSameItemSameComponents(stack, restored),
                            "Broom synchronization and persistence must preserve all components");
                    helper.assertTrue(AbstractBroomItem.getCalibrationSpellData(restored, 0).getLevel() == spell.getMaxLevel(),
                            "Recovered broom scroll must retain its original level");
                    helper.assertFalse(ISpellContainer.isSpellContainer(restored), "Broom must not gain a container");
                }
                player.stopRiding();
                helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).isEmpty(),
                        "Broom scrolls must disappear on dismount");
            } finally {
                player.stopRiding();
                player.discard();
                if (broom != null) broom.discard();
            }
        }
        helper.succeed();
    }

    private static ServerPlayer createRider(GameTestHelper helper) {
        // FakePlayerは騎乗できないため、既存の箒テストと同じ接続付きプレイヤーを使う。
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "broom_transcendence");
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(0, 2, 0));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        MagicData.getPlayerMagicData(player).setMana(10000);
        return player;
    }
}
