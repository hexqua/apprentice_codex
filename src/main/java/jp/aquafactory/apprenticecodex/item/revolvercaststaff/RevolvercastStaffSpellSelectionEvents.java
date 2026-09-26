package jp.aquafactory.apprenticecodex.item.revolvercaststaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.TickEvent;
import io.redspace.ironsspellbooks.setup.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class RevolvercastStaffSpellSelectionEvents {
    private static final Map<UUID, ItemStack[]> SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, CastState> CASTS = new HashMap<>();

    private RevolvercastStaffSpellSelectionEvents() {}

    @SubscribeEvent
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        add(event, event.getEntity().getMainHandItem(), SpellSelectionManager.MAINHAND);
        add(event, event.getEntity().getOffhandItem(), SpellSelectionManager.OFFHAND);
    }

    private static void add(SpellSelectionManager.SpellSelectionEvent event, ItemStack stack, String slot) {
        // 削除待ちの旧投影は Iron's が既に処理しているため、二重に登録しない。
        if (!(stack.getItem() instanceof RevolvercastStaff) || ISpellContainer.isSpellContainer(stack)) return;
        var spell = RevolvercastStaff.getSelectedSpellData(stack);
        if (spell != SpellData.EMPTY) event.addSelectionOption(spell, slot, 0);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) tickPlayer(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforePlayerTick(TickEvent.PlayerTickEvent event) {
        // 詠唱の進行・完了より先に、前 tick 以降の持ち替えと選択変更を検出する。
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) validateCast(player);
    }

    @SubscribeEvent
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) validateCast(player);
    }

    public static void tickPlayer(ServerPlayer player) {
        validateCast(player);
        var previous = SNAPSHOTS.get(player.getUUID());
        var next = new ItemStack[2];
        boolean changed = false;
        for (var hand : InteractionHand.values()) {
            int index = hand.ordinal();
            var held = player.getItemInHand(hand);
            next[index] = held.getItem() instanceof RevolvercastStaff ? held.copy() : ItemStack.EMPTY;
            var old = previous == null ? ItemStack.EMPTY : previous[index];
            if (ItemStack.isSameItemSameTags(old, next[index])) continue;
            changed = true;
        }
        if (next[0].isEmpty() && next[1].isEmpty()) SNAPSHOTS.remove(player.getUUID());
        else SNAPSHOTS.put(player.getUUID(), next);
        if (changed) PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
    }

    public static void onCastStarted(ServerPlayer player, String slot) {
        CASTS.remove(player.getUUID());
        var hand = SpellSelectionManager.MAINHAND.equals(slot) ? InteractionHand.MAIN_HAND
                : SpellSelectionManager.OFFHAND.equals(slot) ? InteractionHand.OFF_HAND : null;
        if (hand == null) return;
        var stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof RevolvercastStaff && MagicData.getPlayerMagicData(player).isCasting()) {
            CASTS.put(player.getUUID(), new CastState(hand, stack, stack.copy()));
        }
    }

    private static void validateCast(ServerPlayer player) {
        var state = CASTS.get(player.getUUID());
        if (state == null) return;
        var magic = MagicData.getPlayerMagicData(player);
        if (!magic.isCasting()) {
            CASTS.remove(player.getUUID());
            return;
        }
        var held = player.getItemInHand(state.hand());
        if (held != state.original() || !sameSelection(state.snapshot(), held)) {
            CASTS.remove(player.getUUID());
            RevolvercastStaffPendingAdvance.clear(player);
            Utils.serverSideCancelCast(player);
        }
    }

    private record CastState(InteractionHand hand, ItemStack original, ItemStack snapshot) {}

    private static boolean sameSelection(ItemStack old, ItemStack current) {
        if (!(old.getItem() instanceof RevolvercastStaff) || !(current.getItem() instanceof RevolvercastStaff)) return false;
        var before = RevolvercastStaff.getSelectedSpellData(old);
        var after = RevolvercastStaff.getSelectedSpellData(current);
        return RevolvercastStaff.getSelectedScrollIndex(old) == RevolvercastStaff.getSelectedScrollIndex(current)
                && before.getSpell() == after.getSpell() && before.getLevel() == after.getLevel();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SNAPSHOTS.remove(event.getEntity().getUUID());
        CASTS.remove(event.getEntity().getUUID());
    }
}
