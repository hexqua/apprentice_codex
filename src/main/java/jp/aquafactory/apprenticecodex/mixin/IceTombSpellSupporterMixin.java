package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import io.redspace.ironsspellbooks.spells.ice.IceTombSpell;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedIceTomb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = IceTombSpell.class, remap = false)
public abstract class IceTombSpellSupporterMixin {
    @WrapOperation(method = "onCast", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Lio/redspace/ironsspellbooks/entity/spells/ice_tomb/IceTombEntity;"))
    private IceTombEntity recordLevel(Level level, Entity owner, Operation<IceTombEntity> original,
                                      Level castLevel, int spellLevel, LivingEntity caster,
                                      CastSource source, MagicData data) {
        var tomb = original.call(level, owner);
        // 非装備で詠唱した墓も、解除までに装備すれば強化対象になる。
        ((SupportedIceTomb) tomb).apprenticecodex$setSpellLevel(spellLevel);
        return tomb;
    }
}
