package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.function.Consumer;

final class ManaShieldCharmLogic {
    private static final float MANA_PER_DAMAGE = 25.0F;
    private static final float DAMAGE_STEP = 1.0F;
    private static final float RECOVERY_THRESHOLD_MANA = 100.0F;
    private static final int VANILLA_INVULNERABLE_TIME_TICKS = 20;
    private static final int VANILLA_COOLDOWN_DAMAGE_GATE_TICKS = VANILLA_INVULNERABLE_TIME_TICKS / 2;

    private ManaShieldCharmLogic() {
    }

    static void onCurioTick(SlotContext slotContext) {
        var entity = slotContext.entity();
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!isPrimaryEquippedCurio(slotContext)) {
            return;
        }

        refreshCooldownIfRecovered(player);
    }

    static void onLivingAttack(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (event.isCanceled()) {
            return;
        }
        if (event.getAmount() <= 0.0F || !player.isAlive()) {
            return;
        }
        if (!isEquippedBy(player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        refreshCooldownIfRecovered(player);

        var state = getState(player);
        if (state == null || state.manualReentryGuard) {
            return;
        }
        if (shouldIgnoreDuringVanillaStyleIFrame(player, event)) {
            event.setCanceled(true);
            return;
        }
        if (state.cooldownActive) {
            return;
        }

        var resolution = resolveDamage(event.getAmount(), magicData.getMana());
        if (resolution.negatedDamage() <= 0.0F) {
            return;
        }

        applyManaResult(player, magicData, resolution.remainingMana());
        event.setCanceled(true);

        switch (resolution.mitigationResult()) {
            case FULLY_NEGATED_ACTIVE -> {
                event.setCanceled(true);
                applyVanillaStyleIFrame(player);
                event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS));
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), false);
            }
            case FULLY_NEGATED_BURNED_OUT -> {
                event.setCanceled(true);
                applyVanillaStyleIFrame(player);
                event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS));
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), true);
            }
            case PARTIALLY_NEGATED_FAILED -> {
                event.setAmount(resolution.remainingDamage());
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), true);
            }
        }
    }

    private static boolean shouldIgnoreDuringVanillaStyleIFrame(ServerPlayer player, LivingIncomingDamageEvent event) {
        return player.invulnerableTime > VANILLA_COOLDOWN_DAMAGE_GATE_TICKS
                && !event.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN);
    }

    private static void applyVanillaStyleIFrame(ServerPlayer player) {
        // LivingAttackEvent を cancel すると通常の hurt 経路が走らず i-frame が付かないため、
        // 完全無効化時だけバニラ相当の無敵時間を明示的に与えて継続接触ダメージの多重消費を防ぐ。
        player.invulnerableTime = Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS);
    }

    static void onDeath(ServerPlayer player) {
        withState(player, ManaShieldCharmState::reset);
    }

    private static DamageResolution resolveDamage(float incomingDamage, float currentMana) {
        var remainingDamage = incomingDamage;
        var remainingMana = currentMana;
        var negatedDamage = 0.0F;

        while (remainingDamage >= DAMAGE_STEP) {
            remainingDamage -= DAMAGE_STEP;
            remainingMana -= MANA_PER_DAMAGE;
            negatedDamage += DAMAGE_STEP;

            if (remainingDamage < DAMAGE_STEP || remainingMana < 0.0F) {
                break;
            }
        }

        return new DamageResolution(negatedDamage, Math.max(remainingDamage, 0.0F), remainingMana);
    }

    private static void applyManaResult(ServerPlayer player, MagicData magicData, float remainingMana) {
        magicData.setMana(Math.max(0.0F, remainingMana));
        if (remainingMana <= 0.0F) {
            withState(player, state -> state.cooldownActive = true);
        }
    }

    private static void refreshCooldownIfRecovered(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() < RECOVERY_THRESHOLD_MANA) {
            return;
        }

        withState(player, state -> state.cooldownActive = false);
    }

    static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.MANA_SHIELD_CHARM.get()))
                .orElse(false);
    }

    private static boolean isPrimaryEquippedCurio(SlotContext slotContext) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.MANA_SHIELD_CHARM.get()))
                .map(slotResult -> slotResult.slotContext().index() == slotContext.index()
                        && slotResult.slotContext().identifier().equals(slotContext.identifier()))
                .orElse(false);
    }

    private static @Nullable ManaShieldCharmState getState(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return null;
        }

        return spellData.get(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE);
    }

    private static void withState(ServerPlayer player, Consumer<ManaShieldCharmState> consumer) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE, consumer);
    }

    private enum MitigationResult {
        FULLY_NEGATED_ACTIVE,
        FULLY_NEGATED_BURNED_OUT,
        PARTIALLY_NEGATED_FAILED
    }

    private record DamageResolution(float negatedDamage, float remainingDamage, float remainingMana) {
        private MitigationResult mitigationResult() {
            if (remainingDamage < DAMAGE_STEP) {
                return remainingMana > 0.0F
                        ? MitigationResult.FULLY_NEGATED_ACTIVE
                        : MitigationResult.FULLY_NEGATED_BURNED_OUT;
            }
            return MitigationResult.PARTIALLY_NEGATED_FAILED;
        }
    }
}
