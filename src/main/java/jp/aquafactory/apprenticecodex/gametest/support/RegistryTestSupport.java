package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public final class RegistryTestSupport {
    private RegistryTestSupport() {}

    public static Block requireForgeBlock(GameTestHelper helper, ResourceLocation id) {
        var block = ForgeRegistries.BLOCKS.getValue(id);
        helper.assertTrue(block != null, "Missing required block for GameTest: " + id);
        return block;
    }

    public static Item requireForgeItem(GameTestHelper helper, ResourceLocation id) {
        var item = ForgeRegistries.ITEMS.getValue(id);
        helper.assertTrue(item != null, "Missing required item for GameTest: " + id);
        return item;
    }
}
