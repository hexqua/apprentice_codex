package jp.aquafactory.apprenticecodex.item.curios.absorptionamplifyamulet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.AbsorptionAmplifyAmuletState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.event.KnockbackControlEvent;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEvent;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.PersistentGameTimeSanitizer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.SlotContext;

final class AbsorptionAmplifyAmuletLogic {
    private static final long RECOVERY_INTERVAL_TICKS = 10L;
    private static final long PROC_COOLDOWN_TICKS = 20L;
    private static final float ABSORPTION_SYNC_EPSILON = 1.0e-4f;
    private static final ResourceLocation ABSORPTION_CAP_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex",
            "absorption_amplify_amulet_cap"
    );

    private AbsorptionAmplifyAmuletLogic() {
    }

    static void onEquip(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        withState(player, state -> {
            var gameTime = player.level().getGameTime();
            state.initialized = true;
            state.targetAbsorption = getTargetAbsorption(player);
            state.lastKnownAbsorption = player.getAbsorptionAmount();
            syncAbsorptionCap(player, state.targetAbsorption);
            scheduleRecovery(state, gameTime);
        });
    }

    static void onUnequip(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        AudioTools.playSoundFromEntity(player.level(),player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS);
        AudioTools.playSoundFromEntity(player.level(),player, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS);
        player.setAbsorptionAmount(0f);
        clearAbsorptionCap(player);
        resetState(player);
    }

    static void onCurioTick(SlotContext slotContext) {
        var entity = slotContext.entity();
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!AbsorptionAmplifyAmulet.isPrimaryEquippedCurio(slotContext)) {
            return;
        }

        withState(player, state -> {
            var gameTime = player.level().getGameTime();
            sanitizePersistentGameTimes(state, gameTime);

            if (!state.initialized) {
                state.initialized = true;
                scheduleRecovery(state, gameTime);
            }

            var nextTargetAbsorption = getTargetAbsorption(player);
            // 1.21.1 では吸収量が MAX_ABSORPTION 上限にクランプされるため、
            // ネックレス由来の上限も必要だが、毎 tick 再設定すると属性再計算が過剰になる。
            if (!isSameAbsorptionValue(state.targetAbsorption, nextTargetAbsorption)) {
                syncAbsorptionCap(player, nextTargetAbsorption);
            }
            state.targetAbsorption = nextTargetAbsorption;

            var currentAbsorption = player.getAbsorptionAmount();

            if (currentAbsorption < state.targetAbsorption
                    && gameTime >= state.recoveryResumeGameTime
                    && gameTime >= state.nextRecoveryGameTime) {
                currentAbsorption = Math.min(state.targetAbsorption, currentAbsorption + 1f);
                player.setAbsorptionAmount(currentAbsorption);
                state.nextRecoveryGameTime = gameTime + RECOVERY_INTERVAL_TICKS;
            }

            state.lastKnownAbsorption = currentAbsorption;
        });
    }

    static void onIncomingAttack(ServerPlayer player, DamageSource source, float amount) {
        if (!shouldProtectIncomingHit(player, amount)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null
                || !magicData.isCasting()
                || magicData.getCastDurationRemaining() <= 0
                || source.is(DamageTypeTagGenerator.LONG_CAST_IGNORE)
                || !magicData.getCastingSpell().getSpell().canBeInterrupted(player)) {
            return;
        }

        // Iron's Spells 'n Spellbooks は被ダメ中断判定で popMarkedPoison を参照しているため、
        // 同じワンショット回避経路を使ってクールダウンを発生させずにこの一撃だけ中断を無効化する。
        magicData.markPoisoned();
    }

    static void onPostDamage(ServerPlayer player, DamageSource source) {
        withState(player, state -> {
            if (!state.initialized) {
                state.lastKnownAbsorption = player.getAbsorptionAmount();
                return;
            }

            var currentAbsorption = player.getAbsorptionAmount();
            if (currentAbsorption < state.lastKnownAbsorption) {
                var gameTime = player.level().getGameTime();
                sanitizePersistentGameTimes(state, gameTime);
                scheduleRecovery(state, gameTime);
                KnockbackControlEvent.markIgnoreNextKnockback(player);

                if (player.invulnerableTime > 0) {
                    player.invulnerableTime *= 2;
                }

                if (gameTime >= state.nextProcGameTime) {
                    state.nextProcGameTime = gameTime + PROC_COOLDOWN_TICKS;
                    ForceFieldDefenseEvent.spawnAbsorbWallEffect(player, source);
                }
            }

            state.lastKnownAbsorption = currentAbsorption;
        });
    }

    static void onDeath(ServerPlayer player) {
        player.setAbsorptionAmount(0f);
        clearAbsorptionCap(player);
        resetState(player);
    }

    private static boolean shouldProtectIncomingHit(ServerPlayer player, float amount) {
        return amount > 0.0f
                && player.isAlive()
                && player.getAbsorptionAmount() > 0.0f
                && AbsorptionAmplifyAmulet.isEquippedBy(player);
    }

    static float getTargetAbsorption(LivingEntity entity) {
        // 既定値8.0は Iron's Spells 'n Spellbooks 3.15.0 の Fortify Lv3 基礎値相当.
        var baseAbsorptionTarget = ApprenticeCodexServerConfig.absorptionAmplifyAmuletBaseAbsorptionTarget();
        var spellPowerModifier = (float) entity.getAttributeValue(AttributeRegistry.SPELL_POWER);
        var holyPowerModifier = (float) SchoolRegistry.HOLY.get().getPowerFor(entity);
        return baseAbsorptionTarget * spellPowerModifier * holyPowerModifier;
    }

    private static void syncAbsorptionCap(ServerPlayer player, float targetAbsorption) {
        var maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null) {
            return;
        }

        maxAbsorption.removeModifier(ABSORPTION_CAP_MODIFIER_ID);
        if (targetAbsorption <= 0f) {
            return;
        }

        maxAbsorption.addTransientModifier(new AttributeModifier(
                ABSORPTION_CAP_MODIFIER_ID,
                targetAbsorption,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void clearAbsorptionCap(ServerPlayer player) {
        var maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null) {
            return;
        }

        maxAbsorption.removeModifier(ABSORPTION_CAP_MODIFIER_ID);
    }

    private static boolean isSameAbsorptionValue(float left, float right) {
        return Math.abs(left - right) < ABSORPTION_SYNC_EPSILON;
    }

    private static void scheduleRecovery(AbsorptionAmplifyAmuletState state, long gameTime) {
        var recoveryDelayTicks = ApprenticeCodexServerConfig.absorptionAmplifyAmuletRecoveryDelayTicks();
        state.recoveryResumeGameTime = gameTime + recoveryDelayTicks;
        state.nextRecoveryGameTime = state.recoveryResumeGameTime;
    }

    private static void sanitizePersistentGameTimes(AbsorptionAmplifyAmuletState state, long gameTime) {
        state.recoveryResumeGameTime = PersistentGameTimeSanitizer.repairPersistedFutureUntilWithKnownMax(
                gameTime,
                state.recoveryResumeGameTime,
                ApprenticeCodexServerConfig.absorptionAmplifyAmuletRecoveryDelayTicks()
        );
        state.nextRecoveryGameTime = PersistentGameTimeSanitizer.repairPersistedFutureUntil(
                gameTime,
                state.nextRecoveryGameTime,
                RECOVERY_INTERVAL_TICKS
        );
        state.nextProcGameTime = PersistentGameTimeSanitizer.repairPersistedFutureUntil(
                gameTime,
                state.nextProcGameTime,
                PROC_COOLDOWN_TICKS
        );
    }

    private static void resetState(ServerPlayer player) {
        withState(player, AbsorptionAmplifyAmuletState::reset);
    }

    private static void withState(ServerPlayer player, java.util.function.Consumer<AbsorptionAmplifyAmuletState> consumer) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.ABSORPTION_AMPLIFY_AMULET_STATE, consumer);
    }
}
