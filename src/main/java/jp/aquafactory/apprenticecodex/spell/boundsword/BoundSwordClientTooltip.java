package jp.aquafactory.apprenticecodex.spell.boundsword;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@OnlyIn(Dist.CLIENT)
public final class BoundSwordClientTooltip {
    private BoundSwordClientTooltip() {
    }

    public static Optional<Component> getStoredItemName(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }

        var instanceId = BoundSwordItem.getInstanceId(stack).orElse(null);
        if (instanceId == null) {
            return Optional.empty();
        }

        var result = new AtomicReference<Component>();
        Capabilities.getSpellData(player).ifPresent(data -> {
            var state = data.get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
            if (!state.active || !instanceId.equals(state.getInstanceId())) {
                return;
            }

            if (BoundSwordItem.isGeneratedForOffhand(stack) && state.hasStoredOffhandStack()) {
                result.set(state.getStoredOffhandStack().getHoverName());
            } else if (state.hasStoredMainhandStack()) {
                result.set(state.getStoredMainhandStack().getHoverName());
            }
        });
        return Optional.ofNullable(result.get());
    }
}
