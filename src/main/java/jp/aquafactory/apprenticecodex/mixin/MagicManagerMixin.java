package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookCastEvents;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshieldRuntime;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MagicManager.class, remap = false)
public abstract class MagicManagerMixin {
    // 独自 CONTINUOUS は構えや duration を独自管理するため、Iron's 標準 tick と二重実行させない。
    @Redirect(
            method = "lambda$tick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;isCasting()Z",
                    ordinal = 0
            )
    )
    private boolean apprentice_codex$skipManagedContinuousInMagicManager(MagicData magicData) {
        return magicData.getSyncedData().isCasting()
                && !FocusStaffbowCastManager.shouldBypassMagicManager(magicData)
                && !BulwarkGreatshieldRuntime.shouldBypassMagicManager(magicData)
                && !ReflectcastShieldRuntime.shouldBypassMagicManager(magicData);
    }

    @Redirect(
            method = "lambda$tick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;castSpell(Lnet/minecraft/world/level/Level;ILnet/minecraft/server/level/ServerPlayer;Lio/redspace/ironsspellbooks/api/spells/CastSource;Z)V",
                    ordinal = 0
            )
    )
    private void apprentice_codex$applyChargecastPowerBonus(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            ServerPlayer player,
            CastSource castSource,
            boolean resetRecastCount
    ) {
        ChargecastCatalystbookCastEvents.castWithPowerBonus(
                spell, level, spellLevel, player, castSource, resetRecastCount
        );
    }

    @Redirect(
            method = "lambda$tick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerCastTick(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"
            )
    )
    private void apprentice_codex$suppressInstantTickDuringChargecast(
            AbstractSpell spell,
            Level level,
            int spellLevel,
            net.minecraft.world.entity.LivingEntity caster,
            MagicData magicData
    ) {
        ChargecastCatalystbookCastEvents.tickSpellUnlessChargecast(
                spell, level, spellLevel, caster, magicData
        );
    }
}
