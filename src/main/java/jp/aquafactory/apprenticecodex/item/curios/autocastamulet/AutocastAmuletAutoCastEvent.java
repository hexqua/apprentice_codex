package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncAutocastAmuletNotificationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AutocastAmuletAutoCastEvent {
    private static final int AUTO_CAST_INTERVAL_TICKS = 20;

    private AutocastAmuletAutoCastEvent() {
    }

    private enum SequenceResult {
        NONE,
        CAST_STARTED,
        BLOCKED
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide || player.tickCount % AUTO_CAST_INTERVAL_TICKS != 0) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var equippedAmulets = getEquippedAmulets(player);
        if (magicData == null) {
            return;
        }
        if (magicData.isCasting()) {
            return;
        }

        for (var slotResult : equippedAmulets) {
            var stack = slotResult.stack();
            if (!(stack.getItem() instanceof AutocastAmulet autocastAmulet)) {
                continue;
            }

            autocastAmulet.initializeSpellContainer(stack);
            autocastAmulet.normalizeImbuedSpellContainer(stack);
            if (AutocastAmulet.isRetrySequenceCoolingDown(stack, player.tickCount)) {
                continue;
            }

            var skippedSlotIndex = AutocastAmulet.consumeReadyRetrySkipSlot(stack, player.tickCount);
            var result = tryCastFirstAvailableSpell(player, magicData, slotResult, autocastAmulet, skippedSlotIndex);
            if (result != SequenceResult.NONE) {
                return;
            }
        }
    }

    private static SequenceResult tryCastFirstAvailableSpell(
            ServerPlayer player,
            MagicData magicData,
            SlotResult slotResult,
            AutocastAmulet autocastAmulet,
            int skippedSlotIndex
    ) {
        var spellContainer = ISpellContainer.get(slotResult.stack());
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return SequenceResult.NONE;
        }

        var activeSpellCount = spellContainer.getActiveSpellCount();
        var castingSlot = String.format("%s_%s", Curios.NECKLACE_SLOT, slotResult.slotContext().index());
        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            if (index == skippedSlotIndex) {
                continue;
            }

            var spellData = spellContainer.getSpellAtIndex(index);
            if (spellData == SpellData.EMPTY || !autocastAmulet.canImbueSpell(spellData)) {
                continue;
            }

            var spell = spellData.getSpell();
            var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
            magicData.resetAdditionalCastData();

            if (spell.requiresLearning() && !spell.isLearned(player)) {
                continue;
            }
            if (magicData.getPlayerCooldowns().isOnCooldown(spell)) {
                continue;
            }

            var scaledManaCost = AutocastAmulet.getScaledManaCost(spell, spellLevel, activeSpellCount);
            if (!player.isCreative() && scaledManaCost > magicData.getMana()) {
                Networks.sendToPlayer(player, new SyncAutocastAmuletNotificationPacket(
                        SyncAutocastAmuletNotificationPacket.NotificationType.MANA_LOW,
                        spell.getSpellId(),
                        0
                ));
                scheduleRetry(slotResult.stack(), player.tickCount, index);
                return SequenceResult.BLOCKED;
            }

            var canCast = spell.canBeCastedBy(spellLevel, CastSource.SWORD, magicData, player);
            if (!canCast.isSuccess()) {
                if (canCast.message != null) {
                    scheduleRetry(slotResult.stack(), player.tickCount, index);
                    return SequenceResult.BLOCKED;
                }
                continue;
            }

            if (!spell.checkPreCastConditions(player.level(), spellLevel, player, magicData)) {
                magicData.resetAdditionalCastData();
                io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, CastSource.SWORD);
                scheduleRetry(slotResult.stack(), player.tickCount, index);
                return SequenceResult.BLOCKED;
            }

            if (!beginAutoCast(player, magicData, slotResult.stack(), spellData, spellLevel, castingSlot, scaledManaCost)) {
                magicData.resetAdditionalCastData();
                continue;
            }

            return SequenceResult.CAST_STARTED;
        }

        return SequenceResult.NONE;
    }

    private static boolean beginAutoCast(
            ServerPlayer player,
            MagicData magicData,
            ItemStack castingStack,
            SpellData spellData,
            int spellLevel,
            String castingSlot,
            int scaledManaCost
    ) {
        var spell = spellData.getSpell();
        if (player.isUsingItem()) {
            player.stopUsingItem();
        }

        // attemptInitiateCast だとこのアイテム独自の skip / cooldown 方針と二重判定になるため、
        // ここでは Iron's 本来の詠唱状態遷移だけを最小限で再利用する。
        if (NeoForge.EVENT_BUS.post(new SpellPreCastEvent(player, spell.getSpellId(), spellLevel, spell.getSchoolType(), CastSource.SWORD))) {
            return false;
        }

        AutocastAmuletManaCostOverrideEvent.reserveManaCostOverride(player, scaledManaCost);
        try {
            var effectiveCastTime = spell.getEffectiveCastTime(spellLevel, player);
            magicData.initiateCast(spell, spellLevel, effectiveCastTime, CastSource.SWORD, castingSlot);
            magicData.setPlayerCastingItem(castingStack);
            spell.onServerPreCast(player.level(), spellLevel, player, magicData);

            PacketDistributor.sendToPlayer(player, new UpdateCastingStatePacket(
                    spell.getSpellId(),
                    spellLevel,
                    effectiveCastTime,
                    CastSource.SWORD,
                    castingSlot
            ));
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new OnCastStartedPacket(
                    player.getUUID(),
                    spell.getSpellId(),
                    spellLevel
            ));

            if (spell.getCastType() == CastType.LONG) {
                TriggeredSpellCastHelper.applyLongCastDurationOverride(player, spellLevel, spell, magicData, castingSlot, 0);
            }
            return true;
        } catch (RuntimeException exception) {
            AutocastAmuletManaCostOverrideEvent.clearManaCostOverride(player);
            throw exception;
        }
    }

    private static void scheduleRetry(ItemStack stack, long currentTick, int spellIndex) {
        AutocastAmulet.scheduleRetrySequence(stack, currentTick, spellIndex);
    }

    private static List<SlotResult> getEquippedAmulets(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof AutocastAmulet).stream()
                        .sorted(Comparator
                                .comparing((SlotResult slotResult) -> slotResult.slotContext().identifier())
                                .thenComparingInt(slotResult -> slotResult.slotContext().index()))
                        .toList())
                .orElse(List.of());
    }

    private static String describeCurioSlot(SlotResult slotResult) {
        return slotResult.slotContext().identifier() + ":" + slotResult.slotContext().index();
    }
}
