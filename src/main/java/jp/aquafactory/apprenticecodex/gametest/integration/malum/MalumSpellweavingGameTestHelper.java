package jp.aquafactory.apprenticecodex.gametest.integration.malum;

import com.sammy.malum.common.entity.activator.SpellweaverToolEffectActivatorEntity;
import com.sammy.malum.common.item.curiosities.tools.spellweaver.SpellweavingPickaxeItem;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class MalumSpellweavingGameTestHelper {
    private MalumSpellweavingGameTestHelper() {
    }

    public static void equipTool(Player player, String toolId, boolean primed) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", toolId));
        var stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        if (primed) {
            SpellweavingPickaxeItem.toggleState(player, InteractionHand.MAIN_HAND);
        }
    }

    public static int countActivators(MiningTestScope scope, Player owner) {
        return (int) scope.entities(SpellweaverToolEffectActivatorEntity.class).stream()
                .filter(entity -> owner.getUUID().equals(entity.getOwner())).count();
    }

    public static Supplier<String> activatorDiagnostics(ServerLevel level, MiningTestScope scope, Player owner) {
        // 検索範囲外への移動や削除後も、生成時に捕捉した実体から運搬状態を調べる。
        var activators = scope.entities(SpellweaverToolEffectActivatorEntity.class).stream()
                .filter(entity -> owner.getUUID().equals(entity.getOwner())).toList();
        return () -> "ownerPos=" + owner.position() + ", ownerAlive=" + owner.isAlive()
                + ", ownerTracked=" + (level.getEntity(owner.getUUID()) == owner)
                + ", activators=" + activators.stream().map(entity -> describeActivator(level, entity)).toList();
    }

    private static String describeActivator(ServerLevel level, SpellweaverToolEffectActivatorEntity entity) {
        var state = new CompoundTag();
        // 内部で保持しているdropは地面のItemEntity検索には現れない。診断にだけ使い、成功条件には足さない。
        try {
            entity.saveWithoutId(state);
            return "{pos=" + entity.position() + ", motion=" + entity.getDeltaMovement()
                    + ", tickCount=" + entity.tickCount + ", removed=" + entity.isRemoved()
                    + ", chunkLoaded=" + level.hasChunkAt(entity.blockPosition())
                    + ", entityTicking=" + level.isPositionEntityTicking(entity.blockPosition())
                    + ", uuidTracked=" + (level.getEntity(entity.getUUID()) == entity)
                    + ", destination=" + entity.getDestination().getTargetLocation()
                    + ", carriedCount=" + state.getInt("carriedItemCount")
                    + ", firstCarriedItem=" + state.get("carriedItem_0") + "}";
        } catch (RuntimeException exception) {
            return "{diagnosticError=" + exception + "}";
        }
    }
}
