package jp.aquafactory.apprenticecodex.gametest.equipment.autocastamulet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import java.util.List;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletAutoCastEvent;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

public final class AutocastAmuletTestSupport {
    private AutocastAmuletTestSupport() {}

    public static void fillSpellContainerToActiveCount(
            GameTestHelper helper,
            ISpellContainerMutable mutable,
            int targetActiveCount
    ) {
        for (var spell : SpellRegistry.getEnabledSpells()) {
            if (mutable.getActiveSpellCount() >= targetActiveCount) {
                break;
            }
            if (mutable.getIndexForSpell(spell) >= 0) {
                continue;
            }

            helper.assertTrue(mutable.addSpell(spell, 1, false),
                    "Failed to prepare overflow test spell: " + spell.getSpellResource());
        }

        helper.assertTrue(mutable.getActiveSpellCount() == targetActiveCount,
                "Failed to prepare overflow Explorer's Codex: expected " + targetActiveCount + " active spells but got "
                        + mutable.getActiveSpellCount());
    }

    static ItemStack createAutocastAmuletStack(GameTestHelper helper, int spellSlotCount, SpellData... spells) {
        var item = (AutocastAmulet) ItemRegistry.AUTOCAST_AMULET.get();
        var stack = item.getDefaultInstance();
        for (var slot = 0; slot < spellSlotCount - AutocastAmulet.MIN_SPELL_SLOTS; ++slot) {
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    slot,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
        }

        helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                "Autocast Amulet setup should not expose Iron's SpellContainer");
        helper.assertTrue(AutocastAmulet.getEnabledSpellSlotCount(stack) == spellSlotCount,
                "Failed to prepare Autocast Amulet with " + spellSlotCount + " enabled spell slots");

        for (var slot = 0; slot < spells.length; ++slot) {
            var spellData = spells[slot];
            helper.assertTrue(item.canImbueSpell(spellData),
                    "Autocast Amulet rejected setup spell " + spellData.getSpell().getSpellResource());
            AutocastAmulet.setCalibrationScroll(stack, slot, createSpellScroll(spellData.getSpell(), spellData.getLevel()));
        }

        return stack;
    }

    public static List<AutoMagnetFamiliarEntity> getOwnedAutoMagnetFamiliars(GameTestHelper helper, FakePlayer owner) {
        return helper.getLevel().getEntitiesOfClass(
                AutoMagnetFamiliarEntity.class,
                new AABB(owner.position(), owner.position()).inflate(32.0),
                familiar -> {
                    var summonOwner = familiar.getOwner();
                    return summonOwner != null && owner.getUUID().equals(summonOwner.getUUID());
                }
        );
    }

    static boolean invokeAutocastBeginCast(
            ServerPlayer player,
            MagicData magicData,
            ItemStack stack,
            SpellData spellData,
            int spellLevel,
            String castingSlot,
            int scaledManaCost
    ) {
        try {
            var method = AutocastAmuletAutoCastEvent.class.getDeclaredMethod(
                    "beginAutoCast",
                    ServerPlayer.class,
                    MagicData.class,
                    ItemStack.class,
                    SpellData.class,
                    int.class,
                    String.class,
                    int.class
            );
            method.setAccessible(true);
            return (boolean) method.invoke(null, player, magicData, stack, spellData, spellLevel, castingSlot, scaledManaCost);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke Autocast Amulet auto-cast helper for GameTest", exception);
        }
    }

    static void runAutocastAmuletServerTick(FakePlayer player, int tickCount) {
        player.tickCount = tickCount;
        AutocastAmuletAutoCastEvent.onPlayerTick(new PlayerTickEvent.Post(player));
        AutocastAmuletCastEvent.onPlayerTick(
                new PlayerTickEvent.Post(player)
        );
    }
}
