package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Player.class)
public abstract class MalumPlayerScytheAttackMixin {
    // Malum 1.6.7 の PlayerMixin は instanceof 判定なので、独自大鎌にも同じ近接 damage type を渡す。
    @ModifyArg(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 0)
    private DamageSource apprenticecodex$useMalumScytheDamage(DamageSource original) {
        var player = (Player) (Object) this;
        if (!player.getMainHandItem().is(ItemRegistry.SPELL_REAPER_SCYTHE.get())) return original;
        var type = ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath("malum", "scythe_melee"));
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), player);
    }
}
