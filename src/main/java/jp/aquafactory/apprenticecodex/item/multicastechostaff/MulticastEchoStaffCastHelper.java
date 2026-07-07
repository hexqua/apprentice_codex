package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MulticastEchoStaffCastHelper {
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

        var echoSpell = player.getEffect(EffectRegistry.ECHO_SPELL);
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
        if (!player.hasEffect(EffectRegistry.ECHO_SPELL) && !isStoredTargetCastDataStillValid(level, magicData)) {
            PRE_CAST_CONTEXTS.remove(player.getUUID());
            return;
        }

        if (!isActiveNormalCastingSequence(magicData, castingItem, spell, spellLevel, castSource)
                || !canStillPassPreCastConditions(spell, level, spellLevel, player, magicData)) {
            PRE_CAST_CONTEXTS.remove(player.getUUID());
            return;
        }

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
    }

    public static void onServerCastComplete(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            ServerPlayer player,
            MagicData magicData,
            boolean cancelled
    ) {
        if (cancelled) {
            discardNormalCastContext(spell, spellLevel, player, magicData);
            return;
        }

        var context = ACTIVE_NORMAL_CASTS.remove(player.getUUID());
        if (!matches(context, spell, spellLevel, magicData)) {
            PRE_CAST_CONTEXTS.remove(player.getUUID());
            return;
        }

        player.removeEffect(EffectRegistry.ECHO_SPELL);
        PRE_CAST_CONTEXTS.remove(player.getUUID());

        var remainingCasts = Math.min(context.amplifier() + 1, ApprenticeCodexServerConfig.multicastEchoStaffMaxMulticastCount());
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

    private static boolean isActiveNormalCastingSequence(
            MagicData magicData,
            ItemStack castingItem,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource
    ) {
        return magicData.isCasting()
                && magicData.getCastingSpellLevel() == spellLevel
                && magicData.getCastingSpell().getSpell().getSpellId().equals(spell.getSpellId())
                && magicData.getCastSource() == castSource
                && magicData.getPlayerCastingItem().getItem() == castingItem.getItem();
    }

    private static boolean isStoredTargetCastDataStillValid(Level level, MagicData magicData) {
        if (!(magicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)
                || !(level instanceof ServerLevel serverLevel)) {
            return true;
        }

        var target = targetData.getTarget(serverLevel);
        return target != null && target.isAlive() && !target.isRemoved();
    }

    private static boolean canStillPassPreCastConditions(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            ServerPlayer player,
            MagicData magicData
    ) {
        var previousCastData = magicData.getAdditionalCastData();
        try {
            // 長詠唱開始時のターゲット情報を再利用すると、完了時点で無効になった対象にもMulticastが続く。
            magicData.setAdditionalCastData(null);
            return spell.checkPreCastConditions(level, spellLevel, player, magicData);
        } finally {
            restoreCastData(magicData, previousCastData);
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
                || player.hasEffect(EffectRegistry.ECHO_SPELL);
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

        var echoSpell = player.getEffect(EffectRegistry.ECHO_SPELL);
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

    private static boolean matches(
            @Nullable ActiveNormalCastContext context,
            AbstractSpell spell,
            int spellLevel,
            MagicData magicData
    ) {
        return matches(context, spell, spellLevel)
                && context.item() == magicData.getPlayerCastingItem().getItem()
                && context.castSource() == magicData.getCastSource();
    }

    private static void discardNormalCastContext(
            AbstractSpell spell,
            int spellLevel,
            ServerPlayer player,
            MagicData magicData
    ) {
        var context = ACTIVE_NORMAL_CASTS.get(player.getUUID());
        if (context != null && matches(context, spell, spellLevel, magicData)) {
            ACTIVE_NORMAL_CASTS.remove(player.getUUID(), context);
        }
        PRE_CAST_CONTEXTS.remove(player.getUUID());
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
            event.setEffectiveCooldown(resolveFinalCooldown(event.getEffectiveCooldown(), event.getSpell(), player, finalContext));
            FINAL_COOLDOWN_CONTEXTS.remove(player.getUUID(), finalContext);
            return;
        }

        var activeContext = ACTIVE_NORMAL_CASTS.get(player.getUUID());
        if (activeContext != null && activeContext.spellId().equals(event.getSpell().getSpellId())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        clearStalePreCastContext(player);
        clearStaleActiveNormalCastContext(player);
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

    private static void clearStaleActiveNormalCastContext(ServerPlayer player) {
        var context = ACTIVE_NORMAL_CASTS.get(player.getUUID());
        if (context == null) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null
                && magicData.isCasting()
                && context.spellId().equals(magicData.getCastingSpell().getSpell().getSpellId())
                && context.spellLevel() == magicData.getCastingSpellLevel()
                && context.item() == magicData.getPlayerCastingItem().getItem()
                && context.castSource() == magicData.getCastSource()) {
            return;
        }

        ACTIVE_NORMAL_CASTS.remove(player.getUUID(), context);
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
        var previousCastData = magicData.getAdditionalCastData();
        try {
            if (canRunMulticastPreCast(level, spell, job.spellLevel(), job.castSource(), player, magicData)) {
                runRepeatedCast(level, spell, job, player, magicData);
            }
        } finally {
            clearRepeatedCastData(magicData, previousCastData);
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

    private static void runRepeatedCast(
            ServerLevel level,
            AbstractSpell spell,
            MulticastJob job,
            ServerPlayer player,
            MagicData magicData
    ) {
        var previousCastData = magicData.getAdditionalCastData();
        try {
            spell.onServerPreCast(level, job.spellLevel(), player, magicData);
            MulticastEchoStaffAttackHandler.runRepeatedCast(
                    player,
                    spell,
                    () -> MulticastEchoStaffMobEffectHandler.runRepeatedCast(
                            player,
                            spell,
                            () -> spell.castSpell(level, job.spellLevel(), player, job.castSource(), false)
                    )
            );
        } finally {
            clearRepeatedCastData(magicData, previousCastData);
        }
    }

    private static void clearRepeatedCastData(MagicData magicData, @Nullable ICastData previousCastData) {
        var currentCastData = magicData.getAdditionalCastData();
        if (currentCastData == previousCastData) {
            return;
        }

        if (currentCastData != null) {
            currentCastData.reset();
        }
        magicData.setAdditionalCastData(previousCastData);
    }

    private static void restoreCastData(MagicData magicData, @Nullable ICastData previousCastData) {
        var currentCastData = magicData.getAdditionalCastData();
        if (currentCastData != null && currentCastData != previousCastData) {
            currentCastData.reset();
        }
        magicData.setAdditionalCastData(previousCastData);
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
                && !NeoForge.EVENT_BUS.post(new SpellPreCastEvent(
                player,
                spell.getSpellId(),
                spellLevel,
                spell.getSchoolType(),
                castSource
        )).isCanceled();
    }

    private static void finishMulticast(ServerPlayer player, MulticastJob job, AbstractSpell spell) {
        MULTICAST_JOBS.remove(player.getUUID(), job);
        var context = new FinalCooldownContext(job.spellId(), job.spellLevel(), job.amplifier());
        FINAL_COOLDOWN_CONTEXTS.put(player.getUUID(), context);
        MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, job.castSource());
        FINAL_COOLDOWN_CONTEXTS.remove(player.getUUID(), context);
    }

    private static int resolveFinalCooldown(
            int baseCooldown,
            AbstractSpell spell,
            ServerPlayer player,
            FinalCooldownContext context
    ) {
        var cooldownCapTicks = ApprenticeCodexServerConfig.multicastEchoStaffCooldownCapTicks();
        if (baseCooldown > cooldownCapTicks) {
            return baseCooldown;
        }

        var cooldownComponent = (context.amplifier() + 2)
                * ApprenticeCodexServerConfig.multicastEchoStaffCooldownMultiplier()
                * baseCooldown;
        var castTimeComponent = (context.amplifier() + 1)
                * ApprenticeCodexServerConfig.multicastEchoStaffCastTimeCooldownMultiplier()
                * spell.getEffectiveCastTime(context.spellLevel(), player);
        return Math.min(cooldownCapTicks, (int) Math.ceil(cooldownComponent + castTimeComponent));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearVolatileState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearVolatileState(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearVolatileState(player);
        }
    }

    private static void clearVolatileState(ServerPlayer player) {
        var playerId = player.getUUID();
        var job = MULTICAST_JOBS.get(playerId);
        if (job != null) {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(job.spellId());
            if (spell != null && spell != io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
                finishMulticast(player, job, spell);
            } else {
                MULTICAST_JOBS.remove(playerId, job);
            }
        }

        PRE_CAST_CONTEXTS.remove(playerId);
        ACTIVE_NORMAL_CASTS.remove(playerId);
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

    private record FinalCooldownContext(String spellId, int spellLevel, int amplifier) {
    }
}
