package jp.aquafactory.apprenticecodex.spell.edgedancer;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@OnlyIn(Dist.CLIENT)
public final class EdgeDancerClientTooltip {
    private EdgeDancerClientTooltip() {
    }

    public static Optional<Component> getStoredItemName(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }

        var instanceId = SpellSideEdgeMirror.getInstanceId(stack).orElse(null);
        if (instanceId == null) {
            return Optional.empty();
        }

        var result = new AtomicReference<Component>();
        player.getCapability(Capabilities.SPELL_DATA).ifPresent(data -> {
            var state = data.get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE);
            if (!state.active || !instanceId.equals(state.getInstanceId()) || !state.hasStoredOffhandStack()) {
                return;
            }

            result.set(state.getStoredOffhandStack().getHoverName());
        });
        return Optional.ofNullable(result.get());
    }
}
