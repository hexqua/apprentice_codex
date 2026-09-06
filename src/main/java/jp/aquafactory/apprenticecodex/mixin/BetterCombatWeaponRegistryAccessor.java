package jp.aquafactory.apprenticecodex.mixin;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WeaponRegistry.class, remap = false)
public interface BetterCombatWeaponRegistryAccessor {
    @Invoker("getAttributes")
    static WeaponAttributes apprenticecodex$getAttributes(ResourceLocation id) {
        throw new AssertionError();
    }
}
