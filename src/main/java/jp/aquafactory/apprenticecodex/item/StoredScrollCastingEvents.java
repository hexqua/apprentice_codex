package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import io.redspace.ironsspellbooks.setup.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** コンテナを持たない手持ちスクロール装備の選択と詠唱寿命を管理する。 */
@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class StoredScrollCastingEvents {
    private static final Map<UUID, ItemStack[]> SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, CastState> CASTS = new HashMap<>();

    private StoredScrollCastingEvents() {}

    public static boolean isTarget(ItemStack stack) {
        return stack.getItem() instanceof ScrollcasterGauntlet || stack.getItem() instanceof ChargecastCatalystbook;
    }

    public static ItemStack resolveHeld(Player player, InteractionHand hand) {
        // 救済対象のGauntletだけ物理offhandを使い、他の装備の両手武器制限は変更しない。
        if (hand == InteractionHand.OFF_HAND && ModList.get().isLoaded("bettercombat")
                && BetterCombatScrollcasterGauntletCompat.isRescueActive(player)) {
            return BetterCombatScrollcasterGauntletCompat.getPhysicalOffhandStack(player);
        }
        return player.getItemInHand(hand);
    }

    public static SpellData selected(ItemStack stack) {
        if (stack.getItem() instanceof ScrollcasterGauntlet) return ScrollcasterGauntlet.getSelectedSpellData(stack);
        if (stack.getItem() instanceof ChargecastCatalystbook) return ChargecastCatalystbook.getSelectedSpellData(stack);
        return SpellData.EMPTY;
    }

    private static int selectedIndex(ItemStack stack) {
        return stack.getItem() instanceof ScrollcasterGauntlet
                ? ScrollcasterGauntlet.getSelectedScrollIndex(stack)
                : ChargecastCatalystbook.getSelectedScrollIndex(stack);
    }

    @SubscribeEvent
    public static void onSelection(SpellSelectionManager.SpellSelectionEvent event) {
        for (var hand : InteractionHand.values()) {
            var stack = resolveHeld(event.getEntity(), hand);
            // 旧投影はIron'sが既に登録している。除去の同期後にイベント登録へ切り替える。
            if (!isTarget(stack) || ISpellContainer.isSpellContainer(stack)) continue;
            var spell = selected(stack);
            if (spell != SpellData.EMPTY) event.addSelectionOption(spell, slot(hand), 0);
        }
    }

    private static String slot(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;
    }

    public static void onCastStarted(ServerPlayer player, String slot) {
        CASTS.remove(player.getUUID());
        var hand = SpellSelectionManager.MAINHAND.equals(slot) ? InteractionHand.MAIN_HAND
                : SpellSelectionManager.OFFHAND.equals(slot) ? InteractionHand.OFF_HAND : null;
        if (hand == null || !MagicData.getPlayerMagicData(player).isCasting()) return;
        var stack = resolveHeld(player, hand);
        if (isTarget(stack)) CASTS.put(player.getUUID(), new CastState(hand, stack, stack.copy()));
    }

    public static void validateCast(ServerPlayer player) {
        var state = CASTS.get(player.getUUID());
        if (state == null) return;
        var magic = MagicData.getPlayerMagicData(player);
        if (!magic.isCasting()) {
            CASTS.remove(player.getUUID());
            return;
        }
        var held = resolveHeld(player, state.hand());
        if (held != state.original() || !sameSelection(state.snapshot(), held)) {
            CASTS.remove(player.getUUID());
            Utils.serverSideCancelCast(player);
        }
    }

    private static boolean sameSelection(ItemStack before, ItemStack after) {
        if (!isTarget(after) || before.getItem() != after.getItem()) return false;
        var oldSpell = selected(before.copy());
        var newSpell = selected(after.copy());
        return selectedIndex(before) == selectedIndex(after)
                && oldSpell.getSpell() == newSpell.getSpell() && oldSpell.getLevel() == newSpell.getLevel();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) validateCast(player);
    }

    @SubscribeEvent
    public static void onEquipment(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) validateCast(player);
    }

    @SubscribeEvent
    public static void afterTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        validateCast(player);
        var previous = SNAPSHOTS.get(player.getUUID());
        var next = new ItemStack[2];
        boolean changed = false;
        for (var hand : InteractionHand.values()) {
            var held = resolveHeld(player, hand);
            int index = hand.ordinal();
            next[index] = isTarget(held) ? held.copy() : ItemStack.EMPTY;
            var old = previous == null ? ItemStack.EMPTY : previous[index];
            changed |= !ItemStack.isSameItemSameTags(old, next[index]);
        }
        if (next[0].isEmpty() && next[1].isEmpty()) SNAPSHOTS.remove(player.getUUID());
        else SNAPSHOTS.put(player.getUUID(), next);
        if (changed) PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SNAPSHOTS.remove(event.getEntity().getUUID());
        CASTS.remove(event.getEntity().getUUID());
    }

    private record CastState(InteractionHand hand, ItemStack original, ItemStack snapshot) {}
}
