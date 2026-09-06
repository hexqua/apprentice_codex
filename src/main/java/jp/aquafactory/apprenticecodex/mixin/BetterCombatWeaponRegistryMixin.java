package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatSpellReaperScytheCompat;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = WeaponRegistry.class, remap = false)
public abstract class BetterCombatWeaponRegistryMixin {
    @Unique
    private static final ResourceLocation SPELLCHARGED_GREATSWORD_CHARGED_ATTRIBUTES =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "spellcharged_greatsword_charged");

    @Shadow
    static Map<ResourceLocation, WeaponAttributes> registrations;

    @Shadow
    static Map<ResourceLocation, AttributesContainer> containers;

    @Inject(
            method = "loadAttributes(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At("TAIL")
    )
    private static void apprenticecodex$registerAdditionalAttributes(
            ResourceManager resourceManager,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback
    ) {
        apprenticecodex$registerAdditionalAttributes(SPELLCHARGED_GREATSWORD_CHARGED_ATTRIBUTES);
        apprenticecodex$registerAdditionalAttributes(BetterCombatSpellReaperScytheCompat.NO_SWEEP_ATTRIBUTES);
    }

    @Unique
    private static void apprenticecodex$registerAdditionalAttributes(ResourceLocation id) {
        var container = containers.get(id);
        if (container == null) {
            return;
        }

        var attributes = WeaponRegistry.resolveAttributes(id, container);
        if (attributes != null) {
            registrations.put(id, attributes);
        }
    }

    @Inject(
            method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void apprenticecodex$useSpellchargedGreatswordChargedAttributes(
            ItemStack stack,
            CallbackInfoReturnable<WeaponAttributes> callback
    ) {
        if (stack != null && jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager.isThrown(stack)) {
            callback.setReturnValue(null);
            return;
        }
        if (!SpellchargedGreatsword.isOverchargeActive(stack)) {
            return;
        }

        var chargedAttributes = registrations.get(SPELLCHARGED_GREATSWORD_CHARGED_ATTRIBUTES);
        if (chargedAttributes != null) {
            callback.setReturnValue(chargedAttributes);
        }
    }
}
