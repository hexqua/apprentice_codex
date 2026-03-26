package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.armor.EnchantressEnchantingTableBonusHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;

@Pseudo
@Mixin(targets = "fuzs.easymagic.world.inventory.ModEnchantmentMenu", remap = false)
public abstract class EasyMagicModEnchantmentMenuMixin {
    @Shadow
    private Player player;

    @Shadow
    @Final
    private List<List<EnchantmentInstance>> clues;

    @Inject(
            method = "updateLevelsAndClues(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("RETURN"),
            require = 1
    )
    private void apprenticecodex$applyEnchantressBonus(RegistryAccess registryAccess, ItemStack itemStack, int enchantingPower, CallbackInfo ci) {
        if (EnchantressEnchantingTableBonusHelper.isFeatureDisabled()) {
            return;
        }

        var menu = (EnchantmentMenu) (Object) this;
        var targetRow = EnchantressEnchantingTableBonusHelper.TARGET_ENCHANT_ROW;
        if (menu.costs[targetRow] <= 0) {
            return;
        }

        var bonus = EnchantressEnchantingTableBonusHelper.getBonusForPlayer(player);
        if (bonus <= 0) {
            return;
        }

        menu.costs[targetRow] += bonus;
        // EasyMagic は候補表示を独自の clues リストで持つため、cost だけ変えると予告と実結果がズレる。
        var updatedClue = apprenticecodex$buildClue(menu, registryAccess, itemStack, targetRow, menu.costs[targetRow]);
        if (updatedClue != null) {
            clues.set(targetRow, updatedClue);
        }
    }

    @SuppressWarnings("unchecked")
    private List<EnchantmentInstance> apprenticecodex$buildClue(
            EnchantmentMenu menu,
            RegistryAccess registryAccess,
            ItemStack itemStack,
            int enchantRow,
            int enchantingLevel
    ) {
        try {
            Method getEnchantmentList = EnchantmentMenu.class.getDeclaredMethod(
                    "getEnchantmentList",
                    RegistryAccess.class,
                    ItemStack.class,
                    int.class,
                    int.class
            );
            getEnchantmentList.setAccessible(true);
            var enchantmentInstances = (List<EnchantmentInstance>) getEnchantmentList.invoke(
                    menu,
                    registryAccess,
                    itemStack,
                    enchantRow,
                    enchantingLevel
            );

            Method createClue = this.getClass().getDeclaredMethod("createClue", List.class);
            createClue.setAccessible(true);
            return (List<EnchantmentInstance>) createClue.invoke(this, enchantmentInstances);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
