package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
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

    private static TagKey<Item> createEnchantableItemTag(String enchantmentPath) {
        // 論理 ID は 1.21.1 と揃え、tags/items と tags/item の配置差は各バージョンの datagen に任せる。
        return createItemTag(enchantmentPath + "_enchantable");
    }

    private static TagKey<EntityType<?>> createEntityTypeTag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name));
    }

    public static final class Blocks {
        private Blocks() {
        }

        public static final TagKey<Block> CAN_RECEIVE_GRACED_RAIN =
                createBlockTag("can_receive_graced_rain");
        public static final TagKey<Block> RIFT_HOLE_TUNNEL_DENYLIST =
                createBlockTag("rift_hole_tunnel_denylist");
        public static final TagKey<Block> HARVEST_MOON_DENYLIST =
                createBlockTag("harvest_moon_denylist");
        public static final TagKey<Block> TREASURE_DIVINATION_TARGETS =
                createBlockTag("treasure_divination_targets");
        public static final TagKey<Block> HEAVENLY_FIST_CRYSTAL_HARVEST_SOURCES =
                createBlockTag("heavenly_fist_crystal_harvest_sources");
        public static final TagKey<Block> HEAVENLY_FIST_CRYSTAL_HARVEST_TARGETS =
                createBlockTag("heavenly_fist_crystal_harvest_targets");
        public static final TagKey<Block> TERRA_RESONANCE_TARGETS =
                createBlockTag("terra_resonance_targets");
        public static final TagKey<Block> TINY_LUMBERJACK_FORCED_LOGS =
                createBlockTag("tiny_lumberjack_forced_logs");
        public static final TagKey<Block> TINY_LUMBERJACK_FORCED_LEAVES =
                createBlockTag("tiny_lumberjack_forced_leaves");
        public static final TagKey<Block> MIST_FORM_PASSABLE =
                createBlockTag("mist_form_passable");
        public static final TagKey<Block> MIST_FORM_IGNORES_MOVEMENT_RESTRICTION =
                createBlockTag("mist_form_ignores_movement_restriction");
        public static final TagKey<Block> LINEAR_BUILD_DENYLIST =
                createBlockTag("linear_build_denylist");
        public static final TagKey<Block> BROOM_FORCED_HOVER_SURFACES =
                createBlockTag("broom_forced_hover_surfaces");
        public static final TagKey<Block> BROOM_DANGEROUS_DISMOUNT_SURFACES =
                createBlockTag("broom_dangerous_dismount_surfaces");
    }

    public static final class Items {
        private Items() {
        }

        public static final TagKey<Item> SPELLCASTER_AMMO_POUCH_STORABLE =
                createItemTag("spellcaster_ammo_pouch_storable");
        public static final TagKey<Item> LUMINOUS_DEVICE_STORABLE =
                createItemTag("luminous_device_storable");
        public static final TagKey<Item> LUMINOUS_DEVICE_CLEAN_UPGRADE_CATALYSTS =
                createItemTag("luminous_device_clean_upgrade_catalysts");
        public static final TagKey<Item> LUMINOUS_DEVICE_CLEAN_UPGRADE_MATERIALS =
                createItemTag("luminous_device_clean_upgrade_materials");
        public static final TagKey<Item> LUMINOUS_DEVICE_MAGE_LIGHT_UPGRADE_MATERIALS =
                createItemTag("luminous_device_mage_light_upgrade_materials");
        public static final TagKey<Item> LUMINOUS_DEVICE_WIZARDLAMP_UPGRADE_MATERIALS =
                createItemTag("luminous_device_wizardlamp_upgrade_materials");
        public static final TagKey<Item> SPELLCASTER_QUIVER_STORABLE =
                createItemTag("spellcaster_quiver_storable");
        public static final TagKey<Item> SPELLCASTER_EMPTY_CASINGS =
                createItemTag("spellcaster_empty_casings");
        public static final TagKey<Item> SPELLCASTER_WORKBENCH_EXTRACTABLE =
                createItemTag("spellcaster_workbench_extractable");
        public static final TagKey<Item> SPELL_DISMANTLEABLE =
                createItemTag("spell_dismantleable");
        public static final TagKey<Item> ASSIST_WINGS_ONLY_JUMP_ITEMS =
                createItemTag("assist_wings_only_jump_items");
        public static final TagKey<Item> SCROLLCASTER_GAUNTLET_SLOT_UPGRADES =
                createItemTag("scrollcaster_gauntlet_slot_upgrades");
        public static final TagKey<Item> SCROLLCASTER_GAUNTLET_SCHOOL_RUNE_DENYLIST =
                createItemTag("scrollcaster_gauntlet_school_rune_denylist");
        public static final TagKey<Item> ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_CATALYSTS =
                createItemTag("archivists_grimoire_row_upgrade_catalysts");
        public static final TagKey<Item> ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_MATERIALS =
                createItemTag("archivists_grimoire_row_upgrade_materials");
        public static final TagKey<Item> SPELL_THROWABLE_CARD_PAPERS =
                createItemTag("spell_throwable_card_papers");
        public static final TagKey<Item> SPELL_INVOKE_CARD_CRAFTING_MATERIALS =
                createItemTag("spell_invoke_card_crafting_materials");
        public static final TagKey<Item> SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS =
                createItemTag("spell_autonomy_card_crafting_materials");
        public static final TagKey<Item> WAND_BASE =
                createItemTag("wand_base");
        public static final TagKey<Item> ALCHEMY_BREWER_HIGH_EFFICIENCY_BASES =
                createItemTag("alchemy_brewer/high_efficiency_bases");
        public static final TagKey<Item> ALCHEMY_BREWER_FAST_BASES =
                createItemTag("alchemy_brewer/fast_bases");
        public static final TagKey<Item> MANA_MENDING_DENYLIST =
                createItemTag("mana_mending_denylist");
        public static final TagKey<Item> MANA_TRANSCRIPTION_REPAIR_COST_RESET_ITEMS =
                createItemTag("mana_transcription_repair_cost_reset_items");
        public static final TagKey<Item> ALACRITY_ENCHANTABLE =
                createEnchantableItemTag("alacrity");
        public static final TagKey<Item> REFLUX_ENCHANTABLE =
                createEnchantableItemTag("reflux");
        public static final TagKey<Item> RESERVOIR_ENCHANTABLE =
                createEnchantableItemTag("reservoir");
        public static final TagKey<Item> SURGE_ENCHANTABLE =
                createEnchantableItemTag("surge");
        public static final TagKey<Item> ATTUNEMENT_ENCHANTABLE =
                createEnchantableItemTag("attunement");
        public static final TagKey<Item> TENSE_ENCHANTABLE =
                createEnchantableItemTag("tense");
        public static final TagKey<Item> TRANSCENDENCE_ENCHANTABLE =
                createEnchantableItemTag("transcendence");
        public static final TagKey<Item> WISDOM_ENCHANTABLE =
                createEnchantableItemTag("wisdom");
        public static final TagKey<Item> PLUNDER_ENCHANTABLE =
                createEnchantableItemTag("plunder");
    }

    public static final class EntityTypes {
        private EntityTypes() {
        }

        public static final TagKey<EntityType<?>> COUNTS_AS_UNDEAD =
                createEntityTypeTag("counts_as_undead");
        public static final TagKey<EntityType<?>> GRAVITY_BOUND_DENYLIST =
                createEntityTypeTag("gravity_bound_denylist");
    }
}
