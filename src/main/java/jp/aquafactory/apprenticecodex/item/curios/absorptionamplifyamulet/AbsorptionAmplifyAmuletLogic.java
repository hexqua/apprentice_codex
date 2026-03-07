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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import top.theillusivec4.curios.api.SlotContext;

final class AbsorptionAmplifyAmuletLogic {
    private static final long RECOVERY_INTERVAL_TICKS = 10L;
    private static final float RECOVERY_MANA_COST = 20f;
    private static final long PROC_COOLDOWN_TICKS = 20L;

    private AbsorptionAmplifyAmuletLogic() {
    }

    static void onEquip(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        withState(player, state -> {
            var gameTime = player.level().getGameTime();
            state.initialized = true;
            state.lastKnownAbsorption = player.getAbsorptionAmount();
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
            var currentAbsorption = player.getAbsorptionAmount();

            if (!state.initialized) {
                state.initialized = true;
                scheduleRecovery(state, gameTime);
            }

            state.targetAbsorption = getTargetAbsorption(player);

            if (currentAbsorption < state.targetAbsorption
                    && gameTime >= state.recoveryResumeGameTime
                    && gameTime >= state.nextRecoveryGameTime) {
                if (consumeRecoveryMana(player)) {
                    var restoredAbsorption = Math.min(state.targetAbsorption, currentAbsorption + 1f);
                    player.setAbsorptionAmount(restoredAbsorption);
                    currentAbsorption = restoredAbsorption;
                }

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
        resetState(player);
    }

    private static boolean shouldProtectIncomingHit(ServerPlayer player, float amount) {
        return amount > 0.0f
                && player.isAlive()
                && player.getAbsorptionAmount() > 0.0f
                && AbsorptionAmplifyAmulet.isEquippedBy(player);
    }

    private static boolean consumeRecoveryMana(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() < RECOVERY_MANA_COST) {
            return false;
        }

        magicData.addMana(-RECOVERY_MANA_COST);
        return true;
    }

    private static float getTargetAbsorption(ServerPlayer player) {
        // 既定値8.0は Iron's Spells 'n Spellbooks 3.15.0 の Fortify Lv3 基礎値相当.
        var baseAbsorptionTarget = ApprenticeCodexServerConfig.absorptionAmplifyAmuletBaseAbsorptionTarget();
        var spellPowerModifier = (float) player.getAttributeValue(AttributeRegistry.SPELL_POWER);
        var holyPowerModifier = (float) SchoolRegistry.HOLY.get().getPowerFor(player);
        return baseAbsorptionTarget * spellPowerModifier * holyPowerModifier;
    }

    private static void scheduleRecovery(AbsorptionAmplifyAmuletState state, long gameTime) {
        var recoveryDelayTicks = ApprenticeCodexServerConfig.absorptionAmplifyAmuletRecoveryDelayTicks();
        state.recoveryResumeGameTime = gameTime + recoveryDelayTicks;
        state.nextRecoveryGameTime = state.recoveryResumeGameTime;
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
