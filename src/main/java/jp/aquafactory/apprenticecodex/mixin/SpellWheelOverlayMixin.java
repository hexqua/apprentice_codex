package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = SpellWheelOverlay.class, remap = false)
public abstract class SpellWheelOverlayMixin {
    @Shadow
    private int wheelSelection;

    @Shadow
    private SpellSelectionManager swsm;

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;timeFromTicks(FI)Ljava/lang/String;"
            ),
            index = 0,
            require = 0
    )
    private float apprenticecodex$adjustDisplayedCooldownForWheel(float cooldownTicks) {
        var player = Minecraft.getInstance().player;
        var selection = swsm == null ? null : swsm.getSpellSlot(wheelSelection);
        if (player == null || selection == null || selection.spellData == null || selection.spellData.getSpell() == null) {
            return cooldownTicks;
        }

        var castingStack = apprentice_codex$resolveSelectionStack(player, selection.slot);
        if (!WeaponImbueCooldownHelper.shouldIgnoreWeaponImbueCooldownMultiplier(
                castingStack,
                selection.spellData.getSpell(),
                selection.getCastSource()
        )) {
            return cooldownTicks;
        }

        return WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                selection.spellData.getSpell(),
                player,
                selection.getCastSource(),
                castingStack
        );
    }

    @Unique
    private static ItemStack apprentice_codex$resolveSelectionStack(Player player, String slot) {
        if (SpellSelectionManager.MAINHAND.equals(slot)) {
            return player.getMainHandItem();
        }
        if (SpellSelectionManager.OFFHAND.equals(slot)) {
            return player.getOffhandItem();
        }
        if (EquipmentSlot.HEAD.getName().equals(slot)) {
            return player.getItemBySlot(EquipmentSlot.HEAD);
        }
        if (EquipmentSlot.CHEST.getName().equals(slot)) {
            return player.getItemBySlot(EquipmentSlot.CHEST);
        }
        if (EquipmentSlot.LEGS.getName().equals(slot)) {
            return player.getItemBySlot(EquipmentSlot.LEGS);
        }
        if (EquipmentSlot.FEET.getName().equals(slot)) {
            return player.getItemBySlot(EquipmentSlot.FEET);
        }
        return ItemStack.EMPTY;
    }
}
