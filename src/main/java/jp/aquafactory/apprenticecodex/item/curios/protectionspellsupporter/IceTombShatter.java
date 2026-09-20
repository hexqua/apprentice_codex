package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.mobs.frozen_humanoid.FrozenHumanoid;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import jp.aquafactory.apprenticecodex.mixin.FrozenHumanoidShatterInvoker;
import net.minecraft.world.entity.LivingEntity;

public final class IceTombShatter {
    private IceTombShatter() {
    }

    public static Runnable prepare(IceTombEntity tomb, int spellLevel) {
        if (tomb.level().isClientSide || spellLevel <= 0
                || !(tomb.getOwner() instanceof LivingEntity caster)
                || !caster.isAlive() || caster.isRemoved() || caster.level() != tomb.level()
                || !ProtectionSpellSupporter.isEquippedBy(caster)) {
            return () -> {};
        }

        // Frost Stepの有効設定とは独立させ、解除時の装備・系統・設定倍率で評価する。
        float damage = SpellRegistry.FROST_STEP_SPELL.get().getSpellPower(spellLevel, caster);
        // 分身はワールドへ追加しない。降車前の姿勢と寸法で元の破砕処理だけを再利用する。
        var shadow = new FrozenHumanoid(tomb.level(), caster);
        shadow.setPos(tomb.position());
        shadow.setShatterDamage(damage);
        return () -> {
            var invoker = (FrozenHumanoidShatterInvoker) shadow;
            invoker.apprenticecodex$doPuffDamage();
            invoker.apprenticecodex$spawnIcicleShards(shadow.getEyePosition(), damage);
        };
    }
}
