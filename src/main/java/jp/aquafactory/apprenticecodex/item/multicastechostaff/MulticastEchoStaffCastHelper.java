package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MulticastEchoStaffCastHelper {
    private static final ConcurrentMap<UUID, LongCastEchoContext> LONG_CAST_CONTEXTS = new ConcurrentHashMap<>();

    private MulticastEchoStaffCastHelper() {
    }

    public static void onServerPreCast(AbstractSpell spell, int spellLevel, LivingEntity entity, MagicData magicData) {
        if (!(entity instanceof ServerPlayer player) || !isEchoTarget(player, magicData, spell, true)) {
            return;
        }

        if (shouldUseNormalCast(spell, spellLevel, player)) {
            sendCannotCastMessage(player, spell);
            return;
        }

        if (spell.getCastType() != CastType.LONG) {
            return;
        }

        var echoSpell = player.getEffect(EffectRegistry.ECHO_SPELL.get());
        if (echoSpell == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        LONG_CAST_CONTEXTS.put(player.getUUID(), new LongCastEchoContext(
                castingItem.getItem(),
                spell.getSpellId(),
                echoSpell.getAmplifier()
        ));
    }

    public static void onCastSpell(AbstractSpell spell, Level level, int spellLevel, ServerPlayer player, MagicData magicData) {
        if (!isEchoTarget(player, magicData, spell, false) || shouldUseNormalCast(spell, spellLevel, player)) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (spell.getCastType() == CastType.LONG) {
            consumeLongCastEcho(player, castingItem, spell);
            return;
        }

        if (spell.getCastType() == CastType.INSTANT) {
            consumeCurrentEcho(player, spell);
        }
    }

    private static boolean isEchoTarget(ServerPlayer player, MagicData magicData, AbstractSpell spell, boolean requireActiveEffect) {
        if (magicData == null
                || spell == null
                || SpellRegistry.ECHO_CAST.get().getSpellId().equals(spell.getSpellId())
                || !(magicData.getPlayerCastingItem().getItem() instanceof MulticastEchoStaff)) {
            return false;
        }

        return !requireActiveEffect
                || player.hasEffect(EffectRegistry.ECHO_SPELL.get());
    }

    private static boolean shouldUseNormalCast(AbstractSpell spell, int spellLevel, ServerPlayer player) {
        return spell.getCastType() == CastType.CONTINUOUS || spell.getRecastCount(spellLevel, player) >= 1;
    }

    private static void consumeCurrentEcho(ServerPlayer player, AbstractSpell spell) {
        var echoSpell = player.getEffect(EffectRegistry.ECHO_SPELL.get());
        if (echoSpell == null) {
            return;
        }

        logEchoAmplifier(player, spell, echoSpell.getAmplifier());
        player.removeEffect(EffectRegistry.ECHO_SPELL.get());
    }

    private static void consumeLongCastEcho(ServerPlayer player, ItemStack castingItem, AbstractSpell spell) {
        var context = LONG_CAST_CONTEXTS.get(player.getUUID());
        if (matches(context, castingItem, spell)) {
            logEchoAmplifier(player, spell, context.amplifier());
            player.removeEffect(EffectRegistry.ECHO_SPELL.get());
            LONG_CAST_CONTEXTS.remove(player.getUUID(), context);
            return;
        }

        var echoSpell = player.getEffect(EffectRegistry.ECHO_SPELL.get());
        if (echoSpell == null) {
            return;
        }

        logEchoAmplifier(player, spell, echoSpell.getAmplifier());
        player.removeEffect(EffectRegistry.ECHO_SPELL.get());
    }

    private static boolean matches(@Nullable LongCastEchoContext context, ItemStack castingItem, AbstractSpell spell) {
        return context != null
                && context.item() == castingItem.getItem()
                && context.spellId().equals(spell.getSpellId());
    }

    private static void logEchoAmplifier(ServerPlayer player, AbstractSpell spell, int amplifier) {
        ApprenticeCodex.LOGGER.info(
                "Multicast Echo Staff consumed EchoSpell amplifier {} for player {} spell {}",
                amplifier,
                player.getGameProfile().getName(),
                spell.getSpellId()
        );
    }

    private static void sendCannotCastMessage(ServerPlayer player, AbstractSpell spell) {
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.multicast_echo_staff.cannot_cast",
                spell.getDisplayName(player)
        ).withStyle(ChatFormatting.RED), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        var context = LONG_CAST_CONTEXTS.get(player.getUUID());
        if (context == null) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.isCasting() && matches(context, magicData.getPlayerCastingItem(), magicData.getCastingSpell().getSpell())) {
            return;
        }

        LONG_CAST_CONTEXTS.remove(player.getUUID(), context);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LONG_CAST_CONTEXTS.remove(event.getEntity().getUUID());
    }

    private record LongCastEchoContext(Item item, String spellId, int amplifier) {
    }
}
