package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.core.handlers.SoulDataHandler;
import com.sammy.malum.visual_effects.networked.MalumNetworkedWeaponParticleEffectType.MalumWeaponParticleEffectBuilder;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheParticleCompat;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.geas.pact.wicked.ReaperGeas", remap = false)
public abstract class MalumReaperGeasParticleMixin {
    @WrapOperation(
            method = "finalizedOutgoingDamageEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sammy/malum/visual_effects/networked/MalumNetworkedWeaponParticleEffectType$MalumWeaponParticleEffectBuilder;color(Lnet/minecraft/world/item/Item;)Lcom/sammy/malum/visual_effects/networked/MalumNetworkedWeaponParticleEffectType$MalumWeaponParticleEffectBuilder;"
            ),
            require = 0
    )
    private MalumWeaponParticleEffectBuilder<?> apprenticecodex$colorSpellReaperScytheCombo(
            MalumWeaponParticleEffectBuilder<?> particle,
            Item item,
            Operation<MalumWeaponParticleEffectBuilder<?>> original,
            LivingDamageEvent.Post event,
            LivingEntity attacker,
            LivingEntity target,
            ItemStack stack
    ) {
        var scytheStack = SoulDataHandler.getScytheWeapon(event.getSource(), attacker);
        if (!scytheStack.is(ItemRegistry.SPELL_REAPER_SCYTHE.get())) {
            return original.call(particle, item);
        }

        // Malum 1.8.2の色解決はItemしか受け取らないため、実際の大鎌StackからImbue学派色を補完する。
        return MalumSpellReaperScytheParticleCompat.applyImbueSchoolColor(particle, scytheStack);
    }
}
