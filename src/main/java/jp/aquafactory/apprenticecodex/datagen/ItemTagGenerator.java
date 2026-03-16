package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ItemTagGenerator extends ItemTagsProvider {
    private static TagKey<Item> createTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static final TagKey<Item> IRONS_STAFF = createTag("irons_spellbooks", "staff");
    private static final TagKey<Item> IRONS_UPGRADE_WHITELIST = createTag("irons_spellbooks", "upgrade_whitelist");
    private static final TagKey<Item> CURIOS_RING = createTag("curios", "ring");
    private static final TagKey<Item> CURIOS_BELT = createTag("curios", "belt");
    private static final TagKey<Item> CURIOS_NECKLACE = createTag("curios", "necklace");
    private static final TagKey<Item> CURIOS_SPELLBOOK = createTag("curios", "spellbook");
    private static final TagKey<Item> MINECRAFT_HEAD_ARMOR = createTag("minecraft", "head_armor");
    private static final TagKey<Item> MINECRAFT_CHEST_ARMOR = createTag("minecraft", "chest_armor");
    private static final TagKey<Item> MINECRAFT_LEG_ARMOR = createTag("minecraft", "leg_armor");
    private static final TagKey<Item> MINECRAFT_FOOT_ARMOR = createTag("minecraft", "foot_armor");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_DURABILITY = createTag("minecraft", "enchantable/durability");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_EQUIPPABLE = createTag("minecraft", "enchantable/equippable");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_MINING_LOOT = createTag("minecraft", "enchantable/mining_loot");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_VANISHING = createTag("minecraft", "enchantable/vanishing");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = createTag("malum", "soul_hunter_weapon");
    private static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = createTag("traveloptics", "can_cast_reversal");
    private static final TagKey<Item> HIDDEN_FROM_RECIPE_VIEWERS = createTag("c", "hidden_from_recipe_viewers");
    private static final TagKey<Item> MAGIC_ITEM_ENCHANTABLE = Enchantments.MAGIC_ITEM_ENCHANTABLE;
    private static final TagKey<Item> OFFHAND_MAGIC_ENCHANTABLE = Enchantments.OFFHAND_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE = Enchantments.OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> SPELL_CONTAINER_MAGIC_ENCHANTABLE = Enchantments.SPELL_CONTAINER_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> SPELL_GUN_ENCHANTABLE = Enchantments.SPELL_GUN_ENCHANTABLE;
    private static final TagKey<Item> TRANSCENDENCE_ENCHANTABLE = Enchantments.TRANSCENDENCE_ENCHANTABLE;
    private static final TagKey<Item> WISDOM_ENCHANTABLE = Enchantments.WISDOM_ENCHANTABLE;

    public ItemTagGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTagLookup,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTagLookup, ApprenticeCodex.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(IRONS_STAFF).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get()
        );
        tag(IRONS_UPGRADE_WHITELIST).add(ItemRegistry.ENDER_GRIMOIRE.get());
        tag(CURIOS_SPELLBOOK).add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.EXPLORERS_CODEX.get(),
                ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get()
        );
        // 1.21.1 のバニラ防具 enchant は item tag 基準になったため、通常防具相当の分類へ入れる.
        tag(MINECRAFT_HEAD_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.ENCHANTRESS_HAT.get()
        );
        tag(MINECRAFT_CHEST_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get()
        );
        tag(MINECRAFT_LEG_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get()
        );
        tag(MINECRAFT_FOOT_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get()
        );
        tag(MINECRAFT_ENCHANTABLE_DURABILITY).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get()
        );
        tag(MINECRAFT_ENCHANTABLE_EQUIPPABLE).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get()
        );
        tag(MINECRAFT_ENCHANTABLE_VANISHING).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get()
        );
        // 1.21.1 のバニラ enchantment JSON は Fortune / Silk Touch を mining_loot タグで判定する.
        tag(MINECRAFT_ENCHANTABLE_MINING_LOOT).add(ItemRegistry.PASTEL_STAFF.get());
        tag(MALUM_SOUL_HUNTER_WEAPON).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get()
        );
        tag(TOMAGIC_REVERSAL_WEAPON).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get()
        );
        tag(MAGIC_ITEM_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                ItemRegistry.DIAMOND_SPELLCASTER_GUN.get(),
                ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                ItemRegistry.COPPER_SPELL_AMPLIFIER.get(),
                ItemRegistry.GOLD_SPELL_AMPLIFIER.get(),
                ItemRegistry.PHOTON_SIPHON.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get()
        );
        // spell gun 専用 enchant は offhand 補助具を巻き込まないように個別タグで分離する.
        tag(SPELL_GUN_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
        );
        // Crystal Bladed Staff は Surge/Attunement などを避けつつ、個別指定の Wisdom/Transcendence のみ許可する。
        tag(WISDOM_ENCHANTABLE).addTag(SPELL_GUN_ENCHANTABLE).add(
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get()
        );
        tag(OFFHAND_MAGIC_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                ItemRegistry.COPPER_SPELL_AMPLIFIER.get(),
                ItemRegistry.GOLD_SPELL_AMPLIFIER.get(),
                ItemRegistry.PHOTON_SIPHON.get()
        );
        tag(OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE)
                .addTag(OFFHAND_MAGIC_ENCHANTABLE)
                .add(
                        ItemRegistry.ENCHANTRESS_HAT.get(),
                        ItemRegistry.ENCHANTRESS_ROBE.get(),
                        ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                        ItemRegistry.ENCHANTRESS_BOOTS.get()
                );
        tag(SPELL_CONTAINER_MAGIC_ENCHANTABLE)
                .addTag(OFFHAND_MAGIC_ENCHANTABLE)
                .addTag(SPELL_GUN_ENCHANTABLE)
                .add(ItemRegistry.ENCHANTRESS_ROBE.get());
        tag(TRANSCENDENCE_ENCHANTABLE)
                .addTag(SPELL_CONTAINER_MAGIC_ENCHANTABLE)
                .add(ItemRegistry.CRYSTAL_BLADED_STAFF.get());

        // 指輪.
        tag(CURIOS_RING).add(
                ItemRegistry.SCARLET_THIRST.get(),
                ItemRegistry.CRAFTSMANS_DELIGHT.get()
        );
        tag(CURIOS_BELT).add(
                ItemRegistry.PROTECTION_SPELL_SUPPORTER.get(),
                ItemRegistry.SPELLCASTER_AMMO_POUCH.get()
        );
        tag(CURIOS_NECKLACE).add(ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get());
        tag(TagRegistry.Items.SPELLCASTER_AMMO_POUCH_STORABLE).add(
                ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get(),
                ItemRegistry.RAPID_SPELLCASTER_ROUND.get(),
                ItemRegistry.BASIC_SPELLCASTER_ROUND.get(),
                ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(),
                ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(),
                ItemRegistry.SPELL_DOMINATOR_ROUND.get()
        );
        tag(TagRegistry.Items.SPELLCASTER_EMPTY_CASINGS).add(
                ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get()
        );
        // 魔法召喚武器はダミー用途のため、JEIなどのレシピビューアから除外する.
        tag(HIDDEN_FROM_RECIPE_VIEWERS).add(
                ItemRegistry.SKY_EDGE_SWORD.get(),
                ItemRegistry.COMMENCE_FIRE_RIFLE.get(),
                ItemRegistry.QUICK_ARMS_HANDGUN.get(),
                ItemRegistry.BREACHING_ENEMY_SHOTGUN.get(),
                ItemRegistry.FLY_SWATTER_LAUNCHER.get(),
                ItemRegistry.THERMAL_PROCESS_THROWER.get()
        );
    }
}
