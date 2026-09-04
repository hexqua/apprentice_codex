package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

        if (player.getAbilities().instabuild) {
            return;
        }

        var cooldownTicks = ApprenticeCodexServerConfig.spellReaperScytheConfig().ascensionCooldownTicks();
        if (cooldownTicks > 0) {
            // ItemCooldownsはItem単位で管理されるため、全Spell Reaper Scytheで同じ待ち時間を共有する。
            original.call(cooldowns, item, cooldownTicks);
        }
    }

}
