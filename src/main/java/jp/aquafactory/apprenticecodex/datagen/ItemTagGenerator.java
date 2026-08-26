package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ItemTagGenerator extends ItemTagsProvider {
    private static TagKey<Item> createTag(String namespace, String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static final TagKey<Item> IRONS_STAFF = createTag("irons_spellbooks", "staff");
    private static final TagKey<Item> IRONS_IMBUE_WHITELIST = createTag("irons_spellbooks", "imbue_whitelist");
    private static final TagKey<Item> IRONS_UPGRADE_WHITELIST = createTag("irons_spellbooks", "upgrade_whitelist");
    private static final TagKey<Item> IRONS_INSCRIBED_RUNE = createTag("irons_spellbooks", "inscribed_rune");
    private static final TagKey<Item> CURIOS_RING = createTag("curios", "ring");
    private static final TagKey<Item> CURIOS_BACK = createTag("curios", "back");
    private static final TagKey<Item> CURIOS_BELT = createTag("curios", "belt");
    private static final TagKey<Item> CURIOS_CHARM = createTag("curios", "charm");
    private static final TagKey<Item> CURIOS_HEAD = createTag("curios", "head");
    private static final TagKey<Item> CURIOS_NECKLACE = createTag("curios", "necklace");
    private static final TagKey<Item> CURIOS_FEET = createTag("curios", "feet");
    private static final TagKey<Item> CURIOS_SPELLBOOK = createTag("curios", "spellbook");
    private static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = createTag("create", "contraption_controlled");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = createTag("malum", "soul_hunter_weapon");
    private static final TagKey<Item> MALUM_HIDDEN_UNTIL_VOID = createTag("malum", "hidden_items/void");
    private static final TagKey<Item> MALUM_HIDDEN_UNTIL_BLACK_CRYSTAL =
            createTag("malum", "hidden_items/black_crystal");
    private static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = createTag("traveloptics", "can_cast_reversal");
    private static final TagKey<Item> HIDDEN_FROM_RECIPE_VIEWERS = createTag("c", "hidden_from_recipe_viewers");
    private static final TagKey<Item> FORGE_BERRIES = createTag("forge", "berries");
    private static final TagKey<Item> FORGE_TOOLS_RANGED_WEAPON = createTag("forge", "tools/ranged_weapon");
    // ローカル名を 1.21.1 側と揃え、Forge 固有のタグ定義場所だけをこの接着部分へ閉じ込める。
    private static final TagKey<Item> ALACRITY_ENCHANTABLE = TagRegistry.Items.ALACRITY_ENCHANTABLE;
    private static final TagKey<Item> REFLUX_ENCHANTABLE = TagRegistry.Items.REFLUX_ENCHANTABLE;
    private static final TagKey<Item> RESERVOIR_ENCHANTABLE = TagRegistry.Items.RESERVOIR_ENCHANTABLE;
    private static final TagKey<Item> SURGE_ENCHANTABLE = TagRegistry.Items.SURGE_ENCHANTABLE;
    private static final TagKey<Item> ATTUNEMENT_ENCHANTABLE = TagRegistry.Items.ATTUNEMENT_ENCHANTABLE;
    private static final TagKey<Item> TENSE_ENCHANTABLE = TagRegistry.Items.TENSE_ENCHANTABLE;
    private static final TagKey<Item> TRANSCENDENCE_ENCHANTABLE = TagRegistry.Items.TRANSCENDENCE_ENCHANTABLE;
    private static final TagKey<Item> WISDOM_ENCHANTABLE = TagRegistry.Items.WISDOM_ENCHANTABLE;
    private static final TagKey<Item> PLUNDER_ENCHANTABLE = TagRegistry.Items.PLUNDER_ENCHANTABLE;

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
        tag(Tags.Items.INGOTS).add(
                ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get(),
                ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get()
        );
        tag(Tags.Items.GEMS).add(ItemRegistry.SPELLSTAINED_DIAMOND.get());
        tag(FORGE_BERRIES).add(ItemRegistry.COMFORT_BERRIES.get());
        tag(Tags.Items.TOOLS_SHIELDS).add(
                ItemRegistry.REFLECTCAST_SHIELD.get(),
                ItemRegistry.PARRYCAST_BUCKLER.get(),
                ItemRegistry.BULWARK_GREATSHIELD.get()
        );
        tag(Tags.Items.TOOLS_BOWS).add(ItemRegistry.ELEMENTAL_BOW.get());
        tag(FORGE_TOOLS_RANGED_WEAPON).add(ItemRegistry.ELEMENTAL_BOW.get());

        tag(TagRegistry.Items.ALCHEMY_BREWER_HIGH_EFFICIENCY_BASES).add(Items.NETHER_WART);
        tag(TagRegistry.Items.ALCHEMY_BREWER_FAST_BASES).add(Items.GLOW_LICHEN);
        tag(IRONS_STAFF).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.MULTICAST_ECHO_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.ILLUMINATE_STELLAR_STAFF.get(),
                ItemRegistry.UNITE_LUNA_STAFF.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.ZENITH_STAFF.get()
        );
        // Iron's Spells の JEI は Imbue 候補収集時に spell_container 未初期化スタックを落とすため、
        // 後付系Curiosアクセ は whitelist へ明示登録して JEI 上でも Arcane Anvil 対象として拾わせる。
        tag(IRONS_IMBUE_WHITELIST).add(
                ItemRegistry.AUTOCAST_AMULET.get(),
                ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get(),
                ItemRegistry.ATTACKCAST_RING.get()
        );

        // ルーンはタグ登録すると除去レシピに自動対応.
        // それ以外はschoolルーンでなければ基本は気にしなくてOK.
        tag(IRONS_INSCRIBED_RUNE).add(
                ItemRegistry.BULLET_RUNE.get()
        );

        var ironsUpgradeWhitelist = tag(IRONS_UPGRADE_WHITELIST);
        tag(MALUM_HIDDEN_UNTIL_VOID).add(
                ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get(),
                ItemRegistry.SOUL_AUGMENTED_WEAVE.get()
        );
        tag(MALUM_HIDDEN_UNTIL_BLACK_CRYSTAL).add(
                ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get(),
                ItemRegistry.SOUL_AUGMENTED_WEAVE.get()
        );

        // 自前の抽象クラスを継承しないアイテムは後の自動収集から漏れるのでここで直接指定する.
        ironsUpgradeWhitelist.add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.SPELLCHARGED_GREATSWORD.get(),
                ItemRegistry.SPELL_SIDE_EDGE.get(),
                ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                ItemRegistry.SCROLLCASTER_GAUNTLET.get(),
                ItemRegistry.CHARGECAST_CATALYSTBOOK.get()
        );

        var malumSoulHunterWeaponTag = tag(MALUM_SOUL_HUNTER_WEAPON);
        malumSoulHunterWeaponTag.add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.MULTICAST_ECHO_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.SPELL_SIDE_EDGE.get(),
                ItemRegistry.SPELLCHARGED_GREATSWORD.get(),
                ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                ItemRegistry.SCROLLCASTER_GAUNTLET.get(),
                ItemRegistry.ZENITH_STAFF.get()
        );

        var tomagicReversalWeaponTag = tag(TOMAGIC_REVERSAL_WEAPON);
        tomagicReversalWeaponTag.add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.MULTICAST_ECHO_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get(),
                ItemRegistry.SMASHCAST_SCEPTER.get(),
                ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                ItemRegistry.MANA_FORCE_BLADE.get(),
                ItemRegistry.SPELLCHARGED_GREATSWORD.get(),
                ItemRegistry.SPELL_SIDE_EDGE.get(),
                ItemRegistry.SCROLLCASTER_GAUNTLET.get(),
                ItemRegistry.ZENITH_STAFF.get()
        );

        var transcendenceEnchantableTag = tag(TRANSCENDENCE_ENCHANTABLE);
        var wisdomEnchantableTag = tag(WISDOM_ENCHANTABLE);
        var plunderEnchantableTag = tag(PLUNDER_ENCHANTABLE);
        var attributeEnchantableTags = Map.of(
                AttributeEnchantmentType.ALACRITY, tag(ALACRITY_ENCHANTABLE),
                AttributeEnchantmentType.REFLUX, tag(REFLUX_ENCHANTABLE),
                AttributeEnchantmentType.RESERVOIR, tag(RESERVOIR_ENCHANTABLE),
                AttributeEnchantmentType.SURGE, tag(SURGE_ENCHANTABLE),
                AttributeEnchantmentType.ATTUNEMENT, tag(ATTUNEMENT_ENCHANTABLE),
                AttributeEnchantmentType.TENSE, tag(TENSE_ENCHANTABLE)
        );

        // 所謂魔法武器全般を自動で登録するようにする.
        for (RegistryObject<Item> itemEntry : ItemRegistry.ITEMS.getEntries()) {
            var item = itemEntry.get();
            if (TranscendencePolicy.supportsDirectApplication(item)) {
                // 1.21.1 側では enchantment JSON の supported_items / primary_items からこのタグを参照する。
                transcendenceEnchantableTag.add(item);
            }
            if (WisdomPolicy.supportsDirectApplication(item)) {
                // 1.21.1 側では enchantment JSON の supported_items / primary_items からこのタグを参照する。
                wisdomEnchantableTag.add(item);
            }
            if (PlunderTarget.supportsDirectApplication(item)) {
                // 1.21.1 側では enchantment JSON の supported_items / primary_items からこのタグを参照する。
                plunderEnchantableTag.add(item);
            }
            for (var type : AttributeEnchantmentType.values()) {
                if (AttributeEnchantmentPolicy.supportsDirectApplication(item, type)) {
                    // 1.21.1 側では各 enchantment JSON の supported_items / primary_items から参照する。
                    attributeEnchantableTags.get(type).add(item);
                }
            }
            if (item instanceof AbstractOffhandMagicItem
                    || item instanceof AbstractSpellGunItem
                    || item instanceof AbstractRightClickMagicWeaponItem) {

                // Iron's 側で upgrade 判定を見るタグは実アイテム列挙しかできないため、
                // ここで抽象クラス継承アイテムを自動収集して追加漏れを防ぐ。
                ironsUpgradeWhitelist.add(item);

                tomagicReversalWeaponTag.add(item);
            }

            if (item instanceof AbstractSpellGunItem
                    || item instanceof AbstractRightClickMagicWeaponItem) {
                // 他の武器互換系も登録する.
                // 1.21.1申し送り事項:CrystalBladedStaffは1.21.1だとStaffItemで登録が漏れるため、別途登録.
                // (重複するとタグが2回出てしまうので1.20.1では個別登録していない)
                malumSoulHunterWeaponTag.add(item);
            }
        }
        tag(CURIOS_SPELLBOOK).add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                ItemRegistry.EXPLORERS_CODEX.get(),
                ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get(),
                ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get()
        );

        tag(CURIOS_RING).add(
                ItemRegistry.SCARLET_THIRST.get(),
                ItemRegistry.CRAFTSMANS_DELIGHT.get(),
                ItemRegistry.SPELL_CAST_PARRYING_RING.get(),
                ItemRegistry.ATTACKCAST_RING.get()
        );
        tag(CURIOS_BELT).add(
                ItemRegistry.PROTECTION_SPELL_SUPPORTER.get(),
                ItemRegistry.SPELLCASTER_AMMO_POUCH.get(),
                ItemRegistry.MAGI_COMPRESSOR_GADGET.get(),
                ItemRegistry.SPELLCASTER_QUIVER.get()
        );
        tag(CURIOS_BACK).add(
                ItemRegistry.SPELLCASTER_QUIVER.get()
        );
        tag(CURIOS_NECKLACE).add(
                ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get(),
                ItemRegistry.AUTOCAST_AMULET.get(),
                ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get()
        );
        tag(CURIOS_HEAD).add(
                ItemRegistry.ASHEN_CIRCLET.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        tag(CURIOS_CHARM).add(ItemRegistry.MANA_SHIELD_CHARM.get());
        tag(CURIOS_FEET).add(
                ItemRegistry.MANA_THRUSTER.get(),
                ItemRegistry.JUMPCAST_CHARM.get()
        );

        tag(CREATE_CONTRAPTION_CONTROLLED).add(
                ItemRegistry.SPELL_DISPENSER.get(),
                ItemRegistry.CREATIVE_SPELL_DISPENSER.get()
        );

        tag(TagRegistry.Items.SPELLCASTER_AMMO_POUCH_STORABLE).add(
                ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get(),
                ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get(),
                ItemRegistry.RAPID_SPELLCASTER_ROUND.get(),
                ItemRegistry.BASIC_SPELLCASTER_ROUND.get(),
                ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(),
                ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(),
                ItemRegistry.SPELL_DOMINATOR_ROUND.get(),
                ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get()
        );
        // 外部 MOD が未導入でもタグ読込を失敗させず、導入時だけ光源を収納対象へ加える。
        tag(TagRegistry.Items.LUMINOUS_DEVICE_STORABLE).add(
                Items.TORCH,
                Items.SOUL_TORCH,
                Items.REDSTONE_TORCH,
                Items.LANTERN,
                Items.SOUL_LANTERN,
                Items.END_ROD,
                Items.SEA_LANTERN,
                Items.REDSTONE_LAMP,
                Items.GLOWSTONE,
                Items.SHROOMLIGHT,
                Items.OCHRE_FROGLIGHT,
                Items.PEARLESCENT_FROGLIGHT,
                Items.VERDANT_FROGLIGHT,
                Items.CAMPFIRE,
                Items.SOUL_CAMPFIRE,
                io.redspace.ironsspellbooks.registries.ItemRegistry.FIREFLY_JAR_ITEM.get(),
                io.redspace.ironsspellbooks.registries.ItemRegistry.BRAZIER_ITEM.get(),
                io.redspace.ironsspellbooks.registries.ItemRegistry.SOUL_BRAZIER_ITEM.get()
        ).addTag(ItemTags.CANDLES)
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("quark", "crystal_lamp"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("supplementaries", "candle_holders"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("supplementaries", "sconces"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("create", "experience_block"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("create", "rose_quartz_lamp"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("quark", "blaze_lantern"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("quark", "duskbound_lantern"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("quark", "stone_lamp"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("quark", "stone_brick_lamp"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("quark", "paper_lantern"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("quark", "paper_lantern_sakura"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("supplementaries", "sconce_lever"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("supplementaries", "stone_lamp"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("supplementaries", "blackstone_lamp"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("supplementaries", "deepslate_lamp"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("supplementaries", "end_stone_lamp"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("supplementaries", "fire_pit"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("suppsquared", "copper_lantern"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("suppsquared", "crimson_lantern"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("suppsquared", "brass_lantern"));
        tag(TagRegistry.Items.LUMINOUS_DEVICE_CLEAN_UPGRADE_CATALYSTS).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get()
        );
        tag(TagRegistry.Items.LUMINOUS_DEVICE_CLEAN_UPGRADE_MATERIALS).add(Items.BRUSH);
        tag(TagRegistry.Items.LUMINOUS_DEVICE_MAGE_LIGHT_UPGRADE_MATERIALS).add(Items.SPYGLASS);
        tag(TagRegistry.Items.LUMINOUS_DEVICE_WIZARDLAMP_UPGRADE_MATERIALS).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()
        );
        tag(TagRegistry.Items.SPELLCASTER_EMPTY_CASINGS).add(
                ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(),
                ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get(),
                ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get()
        );
        tag(TagRegistry.Items.SPELLCASTER_QUIVER_STORABLE).add(
                ItemRegistry.ANTI_MANA_ARROW.get(),
                net.minecraft.world.item.Items.ARROW,
                net.minecraft.world.item.Items.SPECTRAL_ARROW,
                net.minecraft.world.item.Items.TIPPED_ARROW
        );
        tag(net.minecraft.tags.ItemTags.ARROWS).add(ItemRegistry.ANTI_MANA_ARROW.get());
        tag(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE).add(
                ItemRegistry.ENCHANTED_CIRCLET.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.SOULCOLLECTOR_ROBE.get(),
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.MANA_FORCE_BLADE.get()
        );
        tag(TagRegistry.Items.SPELL_DISMANTLEABLE).add(
                net.minecraft.world.item.Items.IRON_SWORD,
                net.minecraft.world.item.Items.DIAMOND_SWORD,
                net.minecraft.world.item.Items.NETHERITE_SWORD
        );
        tag(TagRegistry.Items.ASSIST_WINGS_ONLY_JUMP_ITEMS).add(
                ItemRegistry.SMASHCAST_SCEPTER.get()
        );
        tag(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
        );
        tag(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SCHOOL_RUNE_DENYLIST);
        tag(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_CATALYSTS).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
        );
        tag(TagRegistry.Items.ARCHIVISTS_GRIMOIRE_ROW_UPGRADE_MATERIALS).add(
                io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()
        );
        tag(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS).add(
                net.minecraft.world.item.Items.PAPER
        );
        tag(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS).add(
                net.minecraft.world.item.Items.BLACK_DYE,
                net.minecraft.world.item.Items.INK_SAC,
                net.minecraft.world.item.Items.GLOW_INK_SAC
        );
        tag(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS).add(
                net.minecraft.world.item.Items.ENDER_EYE
        );
        tag(TagRegistry.Items.WAND_BASE).add(net.minecraft.world.item.Items.STICK);
        tag(TagRegistry.Items.MANA_MENDING_DENYLIST);
        tag(TagRegistry.Items.MANA_TRANSCRIPTION_REPAIR_COST_RESET_ITEMS).add(
                ItemRegistry.SPELLSTAINED_DIAMOND.get()
        );

        // 魔法召喚武器はアイテムとして性能を持たずダミーにしか使っていないため、JEIでも表示しないようにする.
        tag(HIDDEN_FROM_RECIPE_VIEWERS).add(
                ItemRegistry.SKY_EDGE_SWORD.get(),
                ItemRegistry.COMMENCE_FIRE_RIFLE.get(),
                ItemRegistry.QUICK_ARMS_HANDGUN.get(),
                ItemRegistry.BREACHING_ENEMY_SHOTGUN.get(),
                ItemRegistry.SILENT_ASSASSIN_RIFLE.get(),
                ItemRegistry.DUAL_ACROBAT_SMG.get(),
                ItemRegistry.LETHAL_ASSAULT_RIFLE.get(),
                ItemRegistry.ARTISAN_SMASH_LAUNCHER.get(),
                ItemRegistry.FLY_SWATTER_LAUNCHER.get(),
                ItemRegistry.THERMAL_PROCESS_THROWER.get()
        );
    }
}
