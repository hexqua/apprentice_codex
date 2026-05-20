package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MulticastEchoStaffCastHelper {
    private static final double FINAL_COOLDOWN_MULTIPLIER_PER_CAST = 1.2D;
    private static final ConcurrentMap<UUID, PreCastEchoContext> PRE_CAST_CONTEXTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, ActiveNormalCastContext> ACTIVE_NORMAL_CASTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, MulticastJob> MULTICAST_JOBS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, FinalCooldownContext> FINAL_COOLDOWN_CONTEXTS = new ConcurrentHashMap<>();

    private MulticastEchoStaffCastHelper() {
    }

    public static void onServerPreCast(AbstractSpell spell, int spellLevel, LivingEntity entity, MagicData magicData) {
        if (!(entity instanceof ServerPlayer player) || !isEchoTarget(player, magicData, spell, true)) {
            return;
        }

        if (isUnsupportedMulticastTarget(spell, spellLevel, player)) {
            sendCannotCastMessage(player, spell);
            return;
        }

        var echoSpell = player.getEffect(EffectRegistry.ECHO_SPELL.get());
        if (echoSpell == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        PRE_CAST_CONTEXTS.put(player.getUUID(), new PreCastEchoContext(
                castingItem.getItem(),
                spell.getSpellId(),
                echoSpell.getAmplifier()
        ));
    }

    public static void onCastSpellStart(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            ServerPlayer player,
            CastSource castSource,
            boolean triggerCooldown,
            MagicData magicData
    ) {
        if (!triggerCooldown || !isEchoTarget(player, magicData, spell, false)
                || isUnsupportedMulticastTarget(spell, spellLevel, player)) {
            PRE_CAST_CONTEXTS.remove(player.getUUID());
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        var amplifier = resolveEchoAmplifier(player, castingItem, spell);
        if (amplifier == null) {
            return;
        }

        var context = new ActiveNormalCastContext(
                castingItem.getItem(),
                spell.getSpellId(),
                spellLevel,
                castSource,
                amplifier
        );
        ACTIVE_NORMAL_CASTS.put(player.getUUID(), context);
        logEchoAmplifier(player, spell, amplifier);
    }

    public static void onCastSpellComplete(AbstractSpell spell, Level level, int spellLevel, ServerPlayer player) {
        var context = ACTIVE_NORMAL_CASTS.remove(player.getUUID());
        if (!matches(context, spell, spellLevel)) {
            return;
        }

        player.removeEffect(EffectRegistry.ECHO_SPELL.get());
        PRE_CAST_CONTEXTS.remove(player.getUUID());

        var remainingCasts = context.amplifier() + 1;
        if (remainingCasts <= 0) {
            finishMulticast(player, context.toFinishedJob(level.getGameTime()), spell);
            return;
        }

        MULTICAST_JOBS.put(player.getUUID(), new MulticastJob(
                context.item(),
                context.spellId(),
                context.spellLevel(),
                context.castSource(),
                context.amplifier(),
                remainingCasts,
                level.getGameTime() + ApprenticeCodexServerConfig.multicastEchoStaffDelayTicks()
        ));
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

    private static boolean isUnsupportedMulticastTarget(AbstractSpell spell, int spellLevel, ServerPlayer player) {
        return (spell.getCastType() != CastType.INSTANT && spell.getCastType() != CastType.LONG)
                || spell.getRecastCount(spellLevel, player) >= 1;
    }

    private static @Nullable Integer resolveEchoAmplifier(ServerPlayer player, ItemStack castingItem, AbstractSpell spell) {
        var context = PRE_CAST_CONTEXTS.get(player.getUUID());
        if (matches(context, castingItem, spell)) {
            return context.amplifier();
        }

        var echoSpell = player.getEffect(EffectRegistry.ECHO_SPELL.get());
        if (echoSpell == null) {
            return null;
        }

        return echoSpell.getAmplifier();
    }

    private static boolean matches(@Nullable PreCastEchoContext context, ItemStack castingItem, AbstractSpell spell) {
        return context != null
                && context.item() == castingItem.getItem()
                && context.spellId().equals(spell.getSpellId());
    }

    private static boolean matches(@Nullable ActiveNormalCastContext context, AbstractSpell spell, int spellLevel) {
        return context != null
                && context.spellId().equals(spell.getSpellId())
                && context.spellLevel() == spellLevel;
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

    private static void sendInsufficientManaMessage(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.multicast_echo_staff.insufficient_mana"
        ).withStyle(ChatFormatting.RED), true);
    }

    private static void sendCancelByItemChangeMessage(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.multicast_echo_staff.cancel_by_item_change"
        ).withStyle(ChatFormatting.RED), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var finalContext = FINAL_COOLDOWN_CONTEXTS.get(player.getUUID());
        if (finalContext != null && finalContext.spellId().equals(event.getSpell().getSpellId())) {
            event.setEffectiveCooldown((int) Math.ceil(event.getEffectiveCooldown() * finalContext.multiplier()));
            FINAL_COOLDOWN_CONTEXTS.remove(player.getUUID(), finalContext);
            return;
        }

        var activeContext = ACTIVE_NORMAL_CASTS.get(player.getUUID());
        if (activeContext != null && activeContext.spellId().equals(event.getSpell().getSpellId())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        clearStalePreCastContext(player);
        tickMulticastJob(player);
    }

    private static void clearStalePreCastContext(ServerPlayer player) {
        var context = PRE_CAST_CONTEXTS.get(player.getUUID());
        if (context == null) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.isCasting() && matches(context, magicData.getPlayerCastingItem(), magicData.getCastingSpell().getSpell())) {
            return;
        }

        PRE_CAST_CONTEXTS.remove(player.getUUID(), context);
    }

    private static void tickMulticastJob(ServerPlayer player) {
        var job = MULTICAST_JOBS.get(player.getUUID());
        if (job == null || player.level().getGameTime() < job.nextCastGameTime()) {
            return;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(job.spellId());
        if (spell == null || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
            MULTICAST_JOBS.remove(player.getUUID(), job);
            return;
        }

        var level = player.serverLevel();
        if (!hasMulticastEchoStaffInHands(player)) {
            sendCancelByItemChangeMessage(player);
            finishMulticast(player, job, spell);
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            MULTICAST_JOBS.remove(player.getUUID(), job);
            return;
        }

        if (requiresManaGate(player, job.castSource()) && magicData.getMana() < spell.getManaCost(job.spellLevel())) {
            sendInsufficientManaMessage(player);
            finishMulticast(player, job, spell);
            return;
        }

        var remainingCasts = job.remainingCasts() - 1;
        if (canRunMulticastPreCast(level, spell, job.spellLevel(), job.castSource(), player, magicData)) {
            spell.onServerPreCast(level, job.spellLevel(), player, magicData);
            spell.castSpell(level, job.spellLevel(), player, job.castSource(), false);
        }

        if (remainingCasts <= 0) {
            finishMulticast(player, job, spell);
            return;
        }

        MULTICAST_JOBS.put(player.getUUID(), job.withProgress(
                remainingCasts,
                level.getGameTime() + ApprenticeCodexServerConfig.multicastEchoStaffDelayTicks()
        ));
    }

    private static boolean hasMulticastEchoStaffInHands(ServerPlayer player) {
        return MulticastEchoStaff.isMulticastEchoStaff(player.getMainHandItem())
                || MulticastEchoStaff.isMulticastEchoStaff(player.getOffhandItem());
    }

    private static boolean requiresManaGate(ServerPlayer player, CastSource castSource) {
        return castSource.consumesMana() && !(player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get());
    }

    private static boolean canRunMulticastPreCast(
            ServerLevel level,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource,
            ServerPlayer player,
            MagicData magicData
    ) {
        return spell.checkPreCastConditions(level, spellLevel, player, magicData)
                && !MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(
                player,
                spell.getSpellId(),
                spellLevel,
                spell.getSchoolType(),
                castSource
        ));
    }

    private static void finishMulticast(ServerPlayer player, MulticastJob job, AbstractSpell spell) {
        MULTICAST_JOBS.remove(player.getUUID(), job);
        var multiplier = (job.amplifier() + 2) * FINAL_COOLDOWN_MULTIPLIER_PER_CAST;
        var context = new FinalCooldownContext(job.spellId(), multiplier);
        FINAL_COOLDOWN_CONTEXTS.put(player.getUUID(), context);
        MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, job.castSource());
        FINAL_COOLDOWN_CONTEXTS.remove(player.getUUID(), context);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearVolatileState(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearVolatileState(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearVolatileState(player.getUUID());
        }
    }

    private static void clearVolatileState(UUID playerId) {
        PRE_CAST_CONTEXTS.remove(playerId);
        ACTIVE_NORMAL_CASTS.remove(playerId);
        MULTICAST_JOBS.remove(playerId);
        FINAL_COOLDOWN_CONTEXTS.remove(playerId);
    }

    private record PreCastEchoContext(Item item, String spellId, int amplifier) {
    }

    private record ActiveNormalCastContext(Item item, String spellId, int spellLevel, CastSource castSource, int amplifier) {
        private MulticastJob toFinishedJob(long gameTime) {
            return new MulticastJob(item, spellId, spellLevel, castSource, amplifier, 0, gameTime);
        }
    }

    private record MulticastJob(
            Item item,
            String spellId,
            int spellLevel,
            CastSource castSource,
            int amplifier,
            int remainingCasts,
            long nextCastGameTime
    ) {
        private MulticastJob withProgress(int remainingCasts, long nextCastGameTime) {
            return new MulticastJob(item, spellId, spellLevel, castSource, amplifier, remainingCasts, nextCastGameTime);
        }
    }

    private record FinalCooldownContext(String spellId, double multiplier) {
    }
}
