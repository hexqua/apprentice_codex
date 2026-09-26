package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.QuickcastCartridgeChargeState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 通常詠唱のライフサイクルを維持し、開始判定の cooldown だけを限定的に迂回する。 */
public final class QuickcastCartridgeCasting {
    public static final String SLOT = "apprenticecodex_quickcast_cartridge";
    private static final Map<UUID, CastState> CASTS = new HashMap<>();
    private static final ThreadLocal<Bypass> BYPASS = new ThreadLocal<>();

    private QuickcastCartridgeCasting() {}

    public static ItemStack findEquipped(Player player) {
        // Item 引数の overload は同 tick の検索結果をキャッシュするため、取り外し直後の再入力で使わない。
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(stack -> stack.is(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get())))
                .map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    public static boolean hasReservation(ServerPlayer player) {
        var state = CASTS.get(player.getUUID());
        return state != null && state.recoveryTicks > 0 && !state.consumed;
    }

    public static boolean bypassCooldown(Player player, AbstractSpell spell) {
        var bypass = BYPASS.get();
        return bypass != null && bypass.needsCharge() && bypass.player() == player && bypass.spell() == spell;
    }

    public static boolean initiate(ServerPlayer player) {
        QuickcastCartridgeCharge.interruptReload(player);
        QuickcastCartridgeCharge.tick(player);
        if (!player.isAlive() || player.isSpectator()
                || MirageAvoidanceEvents.rejectServerInputCastIfLocked(player)) return false;
        var stack = findEquipped(player);
        if (stack.isEmpty()) return false;
        var data = QuickcastScrollCartridge.getResolvedSelectedSpellData(stack);
        if (data == SpellData.EMPTY) return false;
        var spell = data.getSpell();
        var magic = MagicData.getPlayerMagicData(player);
        // 同じ魔法への再入力はキャンセルのみ、別魔法なら既存 quickcast と同様に切り替える。
        if (magic.isCasting()) {
            if (magic.getCastingSpellId().equals(spell.getSpellId())) {
                QuickcastCartridgeCharge.clearConfirmation(player);
                Utils.serverSideCancelCast(player);
                return false;
            }
            CancelCastPacket.cancelCast(player, magic.getCastType() != CastType.LONG);
            if (magic.isCasting()) return false;
        }
        clear(player);
        boolean needsCharge = magic.getPlayerCooldowns().isOnCooldown(spell)
                && !(player.isCreative() && !ServerConfigs.CREATIVE_COOLDOWN.get());
        if (needsCharge && !QuickcastCartridgeCharge.state(player).available(QuickcastCartridgeCharge.now(player))) {
            QuickcastCartridgeCharge.requestReload(player);
            return false;
        }
        QuickcastCartridgeCharge.clearConfirmation(player);
        long animationId = GeoItem.getOrAssignId(stack, player.serverLevel());
        var config = ApprenticeCodexServerConfig.quickcastCartridge();
        long recoveryTicks = needsCharge ? QuickcastCartridgeChargeState.recoveryTicks(
                MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK),
                config.multiplier(), config.minimumTicks()) : 0;
        var state = new CastState(stack, spell.getSpellId(), recoveryTicks);
        CASTS.put(player.getUUID(), state);
        var previous = BYPASS.get();
        boolean started = false;
        try {
            BYPASS.set(new Bypass(player, spell, needsCharge));
            started = spell.attemptInitiateCast(stack, spell.getLevelFor(data.getLevel(), player),
                    player.level(), player, CastSource.SPELLBOOK, true, SLOT);
            if (started) {
                ((QuickcastScrollCartridge) stack.getItem()).triggerAnim(player, animationId, "cartridge", "cast");
            }
            return started;
        } finally {
            if (previous == null) BYPASS.remove(); else BYPASS.set(previous);
            if (!started) clear(player);
            QuickcastCartridgeCharge.sync(player, false, false);
        }
    }

    public static void validate(ServerPlayer player) {
        var state = CASTS.get(player.getUUID());
        if (state == null) return;
        var magic = MagicData.getPlayerMagicData(player);
        if (!magic.isCasting() || !SLOT.equals(magic.getSyncedData().getCastingEquipmentSlot())
                || !state.spellId.equals(magic.getCastingSpellId())) {
            clear(player);
        } else if (state.equipped != findEquipped(player)
                || !ItemStack.isSameItemSameTags(state.stack, findEquipped(player))) {
            Utils.serverSideCancelCast(player);
            clear(player);
        }
    }

    public static void beforeNormalInitiation(ServerPlayer player) {
        QuickcastCartridgeCharge.interruptReload(player);
        // 他の処理が直接 resetCastingState した直後でも、次の魔法へ予約を持ち越さない。
        if (BYPASS.get() == null) validate(player);
    }

    public static void beforeEffect(ServerPlayer player, AbstractSpell spell, CastSource source, MagicData magic) {
        var state = CASTS.get(player.getUUID());
        if (state == null || state.consumed || state.recoveryTicks == 0 || source != CastSource.SPELLBOOK
                || !state.spellId.equals(spell.getSpellId()) || !state.spellId.equals(magic.getCastingSpellId())
                || !SLOT.equals(magic.getSyncedData().getCastingEquipmentSlot())) return;
        // onCast内で終了callbackが走っても消費を失わず、CONTINUOUSの反復でも一回に限定する。
        state.consumed = true;
        QuickcastCartridgeCharge.state(player).consume(QuickcastCartridgeCharge.now(player), state.recoveryTicks);
        QuickcastCartridgeCharge.clearConfirmation(player);
        QuickcastCartridgeCharge.sync(player, false, false);
    }

    public static void clear(ServerPlayer player) {
        CASTS.remove(player.getUUID());
    }

    private record Bypass(Player player, AbstractSpell spell, boolean needsCharge) {}
    private static final class CastState {
        final ItemStack equipped;
        final ItemStack stack;
        final String spellId;
        final long recoveryTicks;
        boolean consumed;
        CastState(ItemStack stack, String spellId, long recoveryTicks) {
            this.equipped = stack;
            this.stack = stack.copy();
            this.spellId = spellId;
            this.recoveryTicks = recoveryTicks;
        }
    }
}
