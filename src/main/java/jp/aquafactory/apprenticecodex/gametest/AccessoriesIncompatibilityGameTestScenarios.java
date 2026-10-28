package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.compat.accessories.AccessoriesCompatibility;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

final class AccessoriesIncompatibilityGameTestScenarios {
    private AccessoriesIncompatibilityGameTestScenarios() {
    }

    static void caseMenuTracksSlotChanges(GameTestHelper helper) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("apprenticecodex.incompatiblesTests") || !AccessoriesCompatibility.isUnsupportedLayerLoaded()) {
            helper.succeed();
            return;
        }
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "acl_menu_probe");
        player.getInventory().setItem(0, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        player.containerMenu = menu;
        int previousMenuSize = menu.slots.size();
        var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
        int previousBackSize = curios.getCurios().get("back").getSlots();
        curios.addTransientSlotModifier("back", ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/incompatible_back"),
                1, AttributeModifier.Operation.ADD_VALUE);
        // 未導入時に外部型を要求せず、Accessoriesが通常使うtick経路で更新通知を発火させる。
        Class.forName("io.wispforest.accessories.impl.AccessoriesEventHandler")
                .getMethod("onLivingEntityTick", LivingEntity.class).invoke(null, player);
        helper.assertTrue(curios.getCurios().get("back").getSlots() == previousBackSize + 1,
                "Probe requires the back slot modifier to apply");
        helper.assertTrue(menu.slots.size() == previousMenuSize + 1,
                "Known incompatibility: Accessories does not rebuild the Spellcaster Accessory Case menu after slot changes");
        helper.succeed();
    }
}
