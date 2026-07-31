package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.TickEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class HighTierSwingcastStaffAttackEvent {
    private static final Map<ServerLevel, Map<UUID, PendingCooldownReduction>> PENDING_REDUCTIONS =
            new WeakHashMap<>();

    private HighTierSwingcastStaffAttackEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }

        var player = resolveDirectPlayerAttack(event);
        if (player == null || !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return;
        }

        recordCooldownReduction(player);
    }

    private static void recordCooldownReduction(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AbstractSwingcastStaffItem staff)) {
            return;
        }

        var reductionTicks = resolveCooldownReductionTicks(staff);
        var spellData = staff.getImbuedSpellData(stack);
        if (reductionTicks <= 0 || spellData == null) {
            return;
        }

        var level = player.serverLevel();
        var gameTime = level.getGameTime();
        var reductionsByPlayer = PENDING_REDUCTIONS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        var pending = reductionsByPlayer.get(player.getUUID());
        if (pending == null || pending.gameTime() != gameTime) {
            reductionsByPlayer.put(player.getUUID(), new PendingCooldownReduction(
                    gameTime,
                    spellData.getSpell().getSpellId(),
                    reductionTicks
            ));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel serverLevel)) {
            return;
        }
        processPendingReductions(serverLevel);
    }

    private static void processPendingReductions(ServerLevel serverLevel) {
        var reductionsByPlayer = PENDING_REDUCTIONS.get(serverLevel);
        if (reductionsByPlayer == null || reductionsByPlayer.isEmpty()) {
            return;
        }

        var gameTime = serverLevel.getGameTime();
        var iterator = reductionsByPlayer.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var pending = entry.getValue();
            if (pending.gameTime() > gameTime) {
                continue;
            }

            applyCooldownReduction(serverLevel, entry.getKey(), pending);
            iterator.remove();
        }

        if (reductionsByPlayer.isEmpty()) {
            PENDING_REDUCTIONS.remove(serverLevel);
        }
    }

    private static void applyCooldownReduction(
            ServerLevel level,
            UUID playerUuid,
            PendingCooldownReduction pending
    ) {
        if (!(level.getPlayerByUUID(playerUuid) instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var cooldowns = magicData.getPlayerCooldowns();
        var cooldown = cooldowns.getSpellCooldowns().get(pending.spellId());
        if (cooldown == null) {
            return;
        }

        if (cooldowns.decrementCooldown(cooldown, pending.reductionTicks())) {
            cooldowns.removeCooldown(pending.spellId());
        }
        cooldowns.syncToPlayer(player);
    }

    private static int resolveCooldownReductionTicks(AbstractSwingcastStaffItem staff) {
        if (staff instanceof DiamondSwingcastStaff) {
            return ApprenticeCodexServerConfig.diamondSwingcastStaffCooldownReductionTicks();
        }
        if (staff instanceof NetheriteSwingcastStaff) {
            return ApprenticeCodexServerConfig.netheriteSwingcastStaffCooldownReductionTicks();
        }
        return 0;
    }

    private static ServerPlayer resolveDirectPlayerAttack(LivingDamageEvent event) {
        ServerPlayer player = null;
        if (event.getSource().getDirectEntity() instanceof ServerPlayer directPlayer) {
            player = directPlayer;
        } else if (event.getSource().getEntity() instanceof ServerPlayer sourcePlayer) {
            player = sourcePlayer;
        }

        if (player == null || event.getSource().getDirectEntity() != player) {
            return null;
        }
        if (!(event.getSource().is(DamageTypes.PLAYER_ATTACK) || "player".equals(event.getSource().getMsgId()))) {
            return null;
        }
        return player;
    }

    private record PendingCooldownReduction(
            long gameTime,
            String spellId,
            int reductionTicks
    ) {
    }
}
