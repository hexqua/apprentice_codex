package jp.aquafactory.apprenticecodex.spell.moonlight;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForge;

public final class MoonLightCounterspellEffect {
    private MoonLightCounterspellEffect() {
    }

    public static void applyAfterSuccessfulDamage(DamageSource source, Entity rawTarget, Entity fallbackCaster) {
        if (rawTarget.level().isClientSide) {
            return;
        }

        var target = CombatTools.resolutePartEntity(rawTarget);
        var caster = source.getEntity() != null ? source.getEntity() : fallbackCaster;

        if (NeoForge.EVENT_BUS.post(new CounterSpellEvent(caster, target)).isCanceled()) {
            return;
        }

        if (target instanceof ServerPlayer serverPlayer) {
            Utils.serverSideCancelCast(serverPlayer, true);
            MagicData.getPlayerMagicData(serverPlayer).getPlayerRecasts().removeAll(RecastResult.COUNTERSPELL);
        }

        if (target instanceof IMagicEntity magicEntity) {
            magicEntity.cancelCast();
        }

        if (target instanceof LivingEntity livingEntity) {
            // NeoForge の牛乳処理をそのまま通し、各効果が宣言した cure と除去イベントを尊重する。
            livingEntity.removeEffectsCuredBy(EffectCures.MILK);

            // 牛乳で消えない魔法効果も Counterspell 本来の対象なので、カテゴリを問わず追加で除去する。
            for (var mobEffect : livingEntity.getActiveEffectsMap().keySet().stream().toList()) {
                if (mobEffect.value() instanceof MagicMobEffect) {
                    livingEntity.removeEffect(mobEffect);
                }
            }
        }
    }
}
