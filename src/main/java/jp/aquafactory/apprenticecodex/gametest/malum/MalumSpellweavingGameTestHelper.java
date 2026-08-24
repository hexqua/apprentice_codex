package jp.aquafactory.apprenticecodex.gametest.malum;

import com.sammy.malum.common.entity.activator.SpellweaverToolEffectActivatorEntity;
import com.sammy.malum.common.item.curiosities.tools.spellweaver.SpellweavingPickaxeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

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

    public static int countActivators(Level level, AABB bounds) {
        return level.getEntitiesOfClass(SpellweaverToolEffectActivatorEntity.class, bounds).size();
    }
}
