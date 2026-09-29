package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import io.redspace.ironsspellbooks.spells.evocation.ShieldSpell;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedShieldEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ShieldSpell.class, remap = false)
public abstract class ShieldSpellSupporterMixin {
    @WrapOperation(method = "onCast", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;F)Lio/redspace/ironsspellbooks/entity/spells/shield/ShieldEntity;"))
    private ShieldEntity createSupportedShield(Level level, float health, Operation<ShieldEntity> original,
                                               Level castLevel, int spellLevel, LivingEntity caster,
                                               CastSource source, MagicData data) {
        if (caster instanceof Player player && ProtectionSpellSupporter.isEquippedBy(player)) {
            var shield = new SupportedShieldEntity(EntityRegistry.SUPPORTED_SHIELD.get(), level);
            shield.setHealth(health);
            shield.setOwner(player);
            return shield;
        }
        return original.call(level, health);
    }
}
