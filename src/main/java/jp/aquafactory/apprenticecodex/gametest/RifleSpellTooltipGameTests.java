package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleScrollStorage;
import jp.aquafactory.apprenticecodex.item.spellgun.RifleSpellTooltipData;
import jp.aquafactory.apprenticecodex.network.packet.SyncEchoProfileSpellIdsPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RifleSpellTooltipGameTests {
    @GameTest(template = "gametest/basic_floor")
    public static void storedScrollsRemainVisibleWithoutMutatingSelection(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        for (boolean multipurpose : List.of(true, false)) {
            var stack = new ItemStack(multipurpose ? ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get()
                    : ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            helper.assertTrue(RifleSpellTooltipData.read(stack, null, lookup).slots().isEmpty(),
                    "Empty rifles must have no scroll entries");
            var scroll = SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1));
            if (multipurpose) MultipurposeStaffrifleScrollStorage.set(stack, 0, scroll, lookup);
            else FullautoRapidcastSpellrifleScrollStorage.set(stack, 0, scroll, lookup);
            stack.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), 1);
            String rootKey = multipurpose ? "MultipurposeStaffrifleCalibration" : "FullautoRapidcastSpellrifleCalibration";
            int maximum = multipurpose ? MultipurposeStaffrifleScrollStorage.MAX_SCROLL_SLOTS
                    : FullautoRapidcastSpellrifleScrollStorage.MAX_SCROLL_SLOTS;
            // 拡張を外した後の保存枠と、古い選択位置を再現する。
            {
                var root = stack.getOrCreateTag();
                var calibration = root.getCompound(rootKey);
                var list = calibration.getList("Scrolls", Tag.TAG_COMPOUND);
                for (int i = 1; i < maximum; i++) {
                    var entry = list.getCompound(0).copy();
                    entry.putInt("Slot", i);
                    list.add(entry);
                }
                calibration.putInt("Selected", maximum - 1);
                root.put(rootKey, calibration);
            }
            var before = stack.copy();
            var data = RifleSpellTooltipData.read(stack, null, lookup);
            helper.assertTrue(data.slots().size() == maximum, "Inactive occupied slots must be included in the count");
            helper.assertTrue(data.selectedSlot() == 0 && data.castSource() == CastSource.SWORD,
                    "An inactive selection must fall back to the first usable slot with sword cooldowns");
            helper.assertTrue(data.selectedSpell().getLevel() == 2,
                    "Transcendence must be applied exactly once: item=" + stack.getItem()
                            + ", actual=" + data.selectedSpell().getLevel() + ", maximum=" + spell.getMaxLevel()
                            + ", enchantment=" + stack.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get()));
            helper.assertTrue(!data.slots().get(data.slots().size() - 1).usable(), "Disabled expansion slots must remain inactive");
            helper.assertTrue(ItemStack.isSameItemSameTags(before, stack), "Tooltip reads must not modify item components");
            for (int i = 0; i < maximum; i++) {
                helper.assertTrue(data.slots().get(i).index() == i, "Scroll entries must remain in slot order");
            }
            if (multipurpose) {
                MultipurposeStaffrifleScrollStorage.select(stack, 1);
                data = RifleSpellTooltipData.read(stack, null, lookup);
                helper.assertTrue(data.selectedSlot() == 1, "Duplicate spell IDs must retain selection by slot");
                var rifle = (MultipurposeStaffrifle) stack.getItem();
                helper.assertTrue(rifle.trySetCalibrationAdjustment(stack, 0,
                        new ItemStack(ItemRegistry.WISDOM_SHARD.get()), lookup), "Wisdom shard must be accepted");
                before = stack.copy();
                data = RifleSpellTooltipData.read(stack, null, lookup);
                helper.assertTrue(data.slots().size() == maximum && data.slots().stream().noneMatch(RifleSpellTooltipData.Slot::usable),
                        "Wisdom shard must preserve all stored entries as inactive");
                helper.assertTrue(data.selectedSlot() == -1 && data.selectedSpell() == SpellData.EMPTY,
                        "Wisdom shard without a player selection must not select an internal scroll");
                helper.assertTrue(ItemStack.isSameItemSameTags(before, stack), "Wisdom tooltip reads must be read-only");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.echo_tooltip_sync")
    public static void echoProfileSyncReplacesAndClearsClientIds(GameTestHelper helper) {
        var first = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var second = SpellRegistry.FIREBOLT_SPELL.get();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet = new SyncEchoProfileSpellIdsPacket(List.of(first.getSpellResource(), second.getSpellResource()));
            SyncEchoProfileSpellIdsPacket.encode(packet, buffer);
            var decoded = SyncEchoProfileSpellIdsPacket.decode(buffer);
            helper.assertTrue(packet.equals(decoded), "Echo profile IDs must survive packet encoding");
            MulticastEchoStaffAttackProfileManager.applyClientSyncedProfileSpellIds(decoded.profileSpellIds());
            helper.assertTrue(MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(first)
                    && MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(second), "Received IDs must be available");
            MulticastEchoStaffAttackProfileManager.applyClientSyncedProfileSpellIds(List.of(second.getSpellResource()));
            helper.assertTrue(!MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(first),
                    "Reload must remove previously supported IDs");
            MulticastEchoStaffAttackProfileManager.clearClientSyncedProfileSpellIds();
            helper.assertTrue(!MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(second),
                    "Logout must clear supported IDs");
        } finally {
            buffer.release();
            MulticastEchoStaffAttackProfileManager.clearClientSyncedProfileSpellIds();
        }
        helper.succeed();
    }
}
