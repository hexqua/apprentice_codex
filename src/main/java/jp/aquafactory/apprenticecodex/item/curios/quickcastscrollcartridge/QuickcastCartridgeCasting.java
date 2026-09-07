package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
    private static final ResourceLocation POWER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "quickcast_cartridge_power");
    private static final Map<UUID, CastState> CASTS = new HashMap<>();
    private static final ThreadLocal<Bypass> BYPASS = new ThreadLocal<>();

    private QuickcastCartridgeCasting() {}

    public static ItemStack findEquipped(Player player) {
        // Item 引数の overload は同 tick の検索結果をキャッシュするため、取り外し直後の再入力で使わない。
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.findFirstCurio(stack -> stack.is(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get())))
                .map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    public static double reduction(int remaining, int total) {
        return total <= 0 ? 0 : Math.max(0, Math.min(1, (double) remaining / total));
    }

    public static boolean bypassCooldown(Player player, AbstractSpell spell) {
        var bypass = BYPASS.get();
        return bypass != null && bypass.player() == player && bypass.spell() == spell;
    }

    public static boolean initiate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()
                || MirageAvoidanceEvents.rejectServerInputCastIfLocked(player)) return false;
        var stack = findEquipped(player);
        if (stack.isEmpty()) return false;
        var data = QuickcastScrollCartridge.getSelectedSpellData(stack);
        if (data == SpellData.EMPTY) return false;
        var spell = data.getSpell();
        var magic = MagicData.getPlayerMagicData(player);
        // 同じ魔法への再入力はキャンセルのみ、別魔法なら既存 quickcast と同様に切り替える。
        if (magic.isCasting()) {
            if (magic.getCastingSpellId().equals(spell.getSpellId())) {
                Utils.serverSideCancelCast(player);
                return false;
            }
            CancelCastPacket.cancelCast(player, magic.getCastType() != CastType.LONG);
            if (magic.isCasting()) return false;
        }
        clear(player);
        long animationId = GeoItem.getOrAssignId(stack, player.serverLevel());
        var cooldown = magic.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
        double penalty = cooldown == null ? 0 : reduction(cooldown.getCooldownRemaining(), cooldown.getSpellCooldown());
        var state = new CastState(stack.copy(), spell.getSpellId());
        CASTS.put(player.getUUID(), state);
        var power = player.getAttribute(AttributeRegistry.SPELL_POWER);
        if (power != null && penalty > 0) {
            power.addTransientModifier(new AttributeModifier(POWER_ID, -penalty,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        var previous = BYPASS.get();
        boolean started = false;
        try {
            BYPASS.set(new Bypass(player, spell));
            started = spell.attemptInitiateCast(stack, spell.getLevelFor(data.getLevel(), player),
                    player.level(), player, CastSource.SPELLBOOK, true, SLOT);
            if (started) {
                ((QuickcastScrollCartridge) stack.getItem()).triggerAnim(player, animationId, "cartridge", "cast");
            }
            return started;
        } finally {
            if (previous == null) BYPASS.remove(); else BYPASS.set(previous);
            if (!started) clear(player);
        }
    }

    public static void validate(ServerPlayer player) {
        var state = CASTS.get(player.getUUID());
        if (state == null) return;
        var magic = MagicData.getPlayerMagicData(player);
        if (!magic.isCasting() || !SLOT.equals(magic.getSyncedData().getCastingEquipmentSlot())
                || !state.spellId().equals(magic.getCastingSpellId())) {
            clear(player);
        } else if (!ItemStack.isSameItemSameComponents(state.stack(), findEquipped(player))) {
            Utils.serverSideCancelCast(player);
            clear(player);
        }
    }

    public static void beforeNormalInitiation(ServerPlayer player) {
        // 他の処理が直接 resetCastingState した直後でも、次の魔法へ modifier を持ち越さない。
        if (BYPASS.get() == null) validate(player);
    }

    public static void clear(ServerPlayer player) {
        CASTS.remove(player.getUUID());
        var power = player.getAttribute(AttributeRegistry.SPELL_POWER);
        if (power != null) power.removeModifier(POWER_ID);
    }

    private record Bypass(Player player, AbstractSpell spell) {}
    private record CastState(ItemStack stack, String spellId) {}
}
