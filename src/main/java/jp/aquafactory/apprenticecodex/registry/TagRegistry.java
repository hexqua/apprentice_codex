package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class TagRegistry {
    public static final class Blocks {
        public static final TagKey<Block> CAN_RECEIVE_GRACED_RAIN =
                TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "can_receive_graced_rain"));
    }
}
