package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.visual_effects.networked.MalumNetworkedWeaponParticleEffectType.MalumWeaponParticleEffectBuilder;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheParticleCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.sammy.malum.core.handlers.enchantment.AscensionHandler", remap = false)
public abstract class MalumAscensionParticleMixin {
    @WrapOperation(
            method = "triggerAscension",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"
            ),
            require = 0
    )
    private static void apprenticecodex$replaceSpellReaperScytheAscensionCooldown(
            ItemCooldowns cooldowns,
            Item item,
            int originalTicks,
            Operation<Void> original,
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack scythe
    ) {
        if (!scythe.is(ItemRegistry.SPELL_REAPER_SCYTHE.get())) {
            original.call(cooldowns, item, originalTicks);
            return;
        }

        // Epic Fightではインネイト入力が発動を管理し、アイテムの使用待ち時間を持ち込まない。
        if (player.getAbilities().instabuild || net.neoforged.fml.ModList.get().isLoaded("epicfight")) {
            return;
        }

        var cooldownTicks = ApprenticeCodexServerConfig.spellReaperScytheConfig().ascensionCooldownTicks();
        if (cooldownTicks > 0) {
            // ItemCooldownsはItem単位で管理されるため、全Spell Reaper Scytheで同じ待ち時間を共有する。
            original.call(cooldowns, item, cooldownTicks);
        }
    }

    @WrapOperation(
            method = "triggerAscension",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sammy/malum/visual_effects/networked/MalumNetworkedWeaponParticleEffectType$MalumWeaponParticleEffectBuilder;color(Lnet/minecraft/world/item/Item;)Lcom/sammy/malum/visual_effects/networked/MalumNetworkedWeaponParticleEffectType$MalumWeaponParticleEffectBuilder;"
            ),
            require = 0
    )
    private static MalumWeaponParticleEffectBuilder<?> apprenticecodex$colorSpellReaperScytheAscension(
            MalumWeaponParticleEffectBuilder<?> particle,
            Item item,
            Operation<MalumWeaponParticleEffectBuilder<?>> original,
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack scythe
    ) {
        if (!scythe.is(ItemRegistry.SPELL_REAPER_SCYTHE.get())) {
            return original.call(particle, item);
        }

        // Malum 1.8.2の色解決はItemしか受け取らないため、実際の大鎌StackからImbue学派色を補完する。
        return MalumSpellReaperScytheParticleCompat.applyImbueSchoolColor(particle, scythe);
    }
}
