package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenStorageStabilizerNameMixin {
    @Redirect(
            method = {"slotChanged", "onNameChanged"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;")
    )
    private Component apprenticecodex$useStorageStabilizerBaseName(ItemStack stack) {
        if (!(stack.getItem() instanceof StorageStabilizer)) {
            return stack.getHoverName();
        }

        // 金床の入力欄では合成後の魔法名を除き、vanilla が独自名として保存する %1$s 部分だけを扱う。
        var displayTag = stack.getTagElement("display");
        if (displayTag != null && displayTag.contains("Name", Tag.TAG_STRING)) {
            try {
                var customName = Component.Serializer.fromJson(displayTag.getString("Name"));
                if (customName != null) {
                    return customName;
                }
            } catch (Exception ignored) {
                // 不正な独自名は vanilla の getHoverName と同様に通常名へフォールバックする。
            }
        }
        return stack.getItem().getName(stack);
    }
}
