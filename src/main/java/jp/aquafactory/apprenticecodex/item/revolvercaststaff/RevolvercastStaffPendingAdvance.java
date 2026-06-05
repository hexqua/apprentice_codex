package jp.aquafactory.apprenticecodex.item.revolvercaststaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.RevolvercastStaff;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class RevolvercastStaffPendingAdvance {
    private static final long PENDING_EXPIRE_TICKS = 40L;
    private static final ConcurrentMap<UUID, PendingAdvance> PENDING_ADVANCES = new ConcurrentHashMap<>();

    private RevolvercastStaffPendingAdvance() {
    }

    public static void reserve(
            ServerPlayer player,
            InteractionHand hand,
            ItemStack stack,
            AbstractSpell spell,
            int selectedIndex
    ) {
        if (!(stack.getItem() instanceof RevolvercastStaff) || selectedIndex < 0) {
            return;
        }

        PENDING_ADVANCES.put(
                player.getUUID(),
                new PendingAdvance(
                        hand,
                        stack.getItem(),
                        spell.getSpellId(),
                        selectedIndex,
                        player.level().getGameTime(),
                        -1L
                )
        );
    }

    public static void clear(ServerPlayer player) {
        PENDING_ADVANCES.remove(player.getUUID());
    }

    public static void onServerCastComplete(
            ServerPlayer player,
            AbstractSpell spell,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        PENDING_ADVANCES.computeIfPresent(player.getUUID(), (ignored, pending) -> {
            var castingItem = playerMagicData.getPlayerCastingItem();
            // Followcast など同時に完了した別詠唱の通知で、Revolvercast の予約を消さない。
            if (!pending.matchesCastingItem(castingItem)) {
                return pending;
            }
            if (!pending.matches(castingItem, spell)) {
                return null;
            }

            if (cancelled) {
                return null;
            }

            return pending.markReady(player.level().getGameTime() + 1L);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        tickPlayer(player);
    }

    public static void tickPlayer(ServerPlayer player) {
        PENDING_ADVANCES.computeIfPresent(player.getUUID(), (ignored, pending) -> {
            var gameTime = player.level().getGameTime();
            if (gameTime - pending.createdGameTime() > PENDING_EXPIRE_TICKS) {
                return null;
            }

            if (!pending.isReady(gameTime)) {
                return pending;
            }

            var magicData = MagicData.getPlayerMagicData(player);
            if (magicData != null && magicData.isCasting()) {
                return pending;
            }

            var stack = player.getItemInHand(pending.hand());
            if (!pending.matches(stack, null)) {
                return null;
            }

            // Iron's は詠唱中の SpellContainer 更新を装備変更として扱う場合があるため、完了後の tick まで遅延する。
            if (RevolvercastStaff.advanceToNextValidScrollIndex(stack)
                    && stack.getItem() instanceof RevolvercastStaff revolvercastStaff) {
                revolvercastStaff.triggerRevolveAnimationIfPossible(player, stack);
            }
            return null;
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_ADVANCES.remove(event.getEntity().getUUID());
    }

    private record PendingAdvance(
            InteractionHand hand,
            Item item,
            String spellId,
            int selectedIndex,
            long createdGameTime,
            long readyGameTime
    ) {
        private PendingAdvance markReady(long readyGameTime) {
            return new PendingAdvance(hand, item, spellId, selectedIndex, createdGameTime, readyGameTime);
        }

        private boolean isReady(long gameTime) {
            return readyGameTime >= 0L && gameTime >= readyGameTime;
        }

        private boolean matchesCastingItem(ItemStack stack) {
            return stack.getItem() == item && stack.getItem() instanceof RevolvercastStaff;
        }

        private boolean matches(ItemStack stack, AbstractSpell completedSpell) {
            if (!matchesCastingItem(stack)) {
                return false;
            }

            if (RevolvercastStaff.getSelectedScrollIndex(stack) != selectedIndex) {
                return false;
            }

            var selectedSpellData = RevolvercastStaff.getSelectedSpellData(stack);
            if (selectedSpellData == null || selectedSpellData == SpellData.EMPTY) {
                return false;
            }

            if (!spellId.equals(selectedSpellData.getSpell().getSpellId())) {
                return false;
            }

            return completedSpell == null || spellId.equals(completedSpell.getSpellId());
        }
    }
}
