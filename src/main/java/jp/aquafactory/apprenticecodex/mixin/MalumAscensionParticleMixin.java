package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScytheClientConfigState;
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
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.enchantment.scythe.AscensionEnchantment", remap = false)
public abstract class MalumAscensionParticleMixin {
    @Redirect(
            method = "triggerAscension",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V", remap = true
            ),
            require = 0
    )
    private static void apprenticecodex$replaceSpellReaperScytheAscensionCooldown(
            ItemCooldowns cooldowns,
            Item item,
            int originalTicks,
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack scythe
    ) {
        if (!scythe.is(ItemRegistry.SPELL_REAPER_SCYTHE.get())) {
            cooldowns.addCooldown(item, originalTicks);
            return;
        }

        // Epic Fightではインネイト入力が発動を管理し、アイテムの使用待ち時間を持ち込まない。
        if (player.getAbilities().instabuild || net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) {
            return;
        }

        var cooldownTicks = (level.isClientSide
                ? SpellReaperScytheClientConfigState.values()
                : ApprenticeCodexServerConfig.spellReaperScytheConfig()).ascensionCooldownTicks();
        if (cooldownTicks > 0) {
            // ItemCooldownsはItem単位で管理されるため、全Spell Reaper Scytheで同じ待ち時間を共有する。
            cooldowns.addCooldown(item, cooldownTicks);
        }
    }

}
