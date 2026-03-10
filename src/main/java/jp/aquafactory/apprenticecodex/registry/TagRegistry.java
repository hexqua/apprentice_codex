package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class TagRegistry {
    private TagRegistry() {
    }

    private static TagKey<Block> createBlockTag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name));
    }

    public static final class Blocks {
        private Blocks() {
        }

        public static final TagKey<Block> CAN_RECEIVE_GRACED_RAIN =
                createBlockTag("can_receive_graced_rain");
    }
}
