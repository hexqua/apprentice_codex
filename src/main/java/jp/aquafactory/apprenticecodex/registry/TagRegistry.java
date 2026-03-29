package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class TagRegistry {
    private TagRegistry() {
    }

    private static TagKey<Block> createBlockTag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name));
    }

    private static TagKey<Item> createItemTag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name));
    }

    public static final class Blocks {
        private Blocks() {
        }

        public static final TagKey<Block> CAN_RECEIVE_GRACED_RAIN =
                createBlockTag("can_receive_graced_rain");
        public static final TagKey<Block> RIFT_HOLE_TUNNEL_DENYLIST =
                createBlockTag("rift_hole_tunnel_denylist");
        public static final TagKey<Block> TINY_LUMBERJACK_FORCED_LOGS =
                createBlockTag("tiny_lumberjack_forced_logs");
        public static final TagKey<Block> TINY_LUMBERJACK_FORCED_LEAVES =
                createBlockTag("tiny_lumberjack_forced_leaves");
    }

    public static final class Items {
        private Items() {
        }

        public static final TagKey<Item> SPELLCASTER_AMMO_POUCH_STORABLE =
                createItemTag("spellcaster_ammo_pouch_storable");
        public static final TagKey<Item> SPELLCASTER_EMPTY_CASINGS =
                createItemTag("spellcaster_empty_casings");
    }
}
