package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
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
    private static final TagKey<Item> IRONS_IMBUE_WHITELIST = createTag("irons_spellbooks", "imbue_whitelist");
    private static final TagKey<Item> IRONS_UPGRADE_WHITELIST = createTag("irons_spellbooks", "upgrade_whitelist");
    private static final TagKey<Item> CURIOS_RING = createTag("curios", "ring");
    private static final TagKey<Item> CURIOS_BACK = createTag("curios", "back");
    private static final TagKey<Item> CURIOS_BELT = createTag("curios", "belt");
    private static final TagKey<Item> CURIOS_CHARM = createTag("curios", "charm");
    private static final TagKey<Item> CURIOS_HEAD = createTag("curios", "head");
    private static final TagKey<Item> CURIOS_NECKLACE = createTag("curios", "necklace");
    private static final TagKey<Item> CURIOS_SPELLBOOK = createTag("curios", "spellbook");
    private static final TagKey<Item> CREATE_CONTRAPTION_CONTROLLED = createTag("create", "contraption_controlled");
    private static final TagKey<Item> MINECRAFT_HEAD_ARMOR = createTag("minecraft", "head_armor");
    private static final TagKey<Item> MINECRAFT_CHEST_ARMOR = createTag("minecraft", "chest_armor");
    private static final TagKey<Item> MINECRAFT_LEG_ARMOR = createTag("minecraft", "leg_armor");
    private static final TagKey<Item> MINECRAFT_FOOT_ARMOR = createTag("minecraft", "foot_armor");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_BOW = createTag("minecraft", "enchantable/bow");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_SWORD = createTag("minecraft", "enchantable/sword");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_FIRE_ASPECT = createTag("minecraft", "enchantable/fire_aspect");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_SHARP_WEAPON = createTag("minecraft", "enchantable/sharp_weapon");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_WEAPON = createTag("minecraft", "enchantable/weapon");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_DURABILITY = createTag("minecraft", "enchantable/durability");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_EQUIPPABLE = createTag("minecraft", "enchantable/equippable");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_MINING_LOOT = createTag("minecraft", "enchantable/mining_loot");
    private static final TagKey<Item> MINECRAFT_ENCHANTABLE_VANISHING = createTag("minecraft", "enchantable/vanishing");
    private static final TagKey<Item> MALUM_MAGIC_CAPABLE_WEAPON = createTag("malum", "magic_capable_weapon");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = createTag("malum", "soul_hunter_weapon");
    private static final TagKey<Item> MALUM_SOUL_SHATTER_CAPABLE_WEAPON = createTag("malum", "soul_shatter_capable_weapon");
    private static final TagKey<Item> TOMAGIC_REVERSAL_WEAPON = createTag("traveloptics", "can_cast_reversal");
    private static final TagKey<Item> HIDDEN_FROM_RECIPE_VIEWERS = createTag("c", "hidden_from_recipe_viewers");
    private static final TagKey<Item> MAGIC_ITEM_ENCHANTABLE = Enchantments.MAGIC_ITEM_ENCHANTABLE;
    private static final TagKey<Item> OFFHAND_MAGIC_ENCHANTABLE = Enchantments.OFFHAND_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE = Enchantments.OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> SPELL_CONTAINER_MAGIC_ENCHANTABLE = Enchantments.SPELL_CONTAINER_MAGIC_ENCHANTABLE;
    private static final TagKey<Item> SPELL_GUN_ENCHANTABLE = Enchantments.SPELL_GUN_ENCHANTABLE;
    private static final TagKey<Item> DRINKABLE_FLASK_ENCHANTABLE = Enchantments.DRINKABLE_FLASK_ENCHANTABLE;
    private static final TagKey<Item> ALCHEMISTS_FLASK_ENCHANTABLE = Enchantments.ALCHEMISTS_FLASK_ENCHANTABLE;
    private static final TagKey<Item> FLASK_ENCHANTABLE = Enchantments.FLASK_ENCHANTABLE;
    private static final TagKey<Item> TRANSCENDENCE_ENCHANTABLE = Enchantments.TRANSCENDENCE_ENCHANTABLE;
    private static final TagKey<Item> WISDOM_ENCHANTABLE = Enchantments.WISDOM_ENCHANTABLE;
    private static final TagKey<Item> PLUNDER_ENCHANTABLE = Enchantments.PLUNDER_ENCHANTABLE;
    private static final TagKey<Item> SYNTHESIS_ENCHANTABLE = Enchantments.SYNTHESIS_ENCHANTABLE;
    private static final TagKey<Item> MANA_SHIELD_CHARM_ENCHANTABLE = Enchantments.MANA_SHIELD_CHARM_ENCHANTABLE;

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
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.ILLUMINATE_STELLAR_STAFF.get(),
                ItemRegistry.UNITE_LUNA_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get()
        );
        // Iron's Spells の JEI は Imbue 候補収集時に spell_container 未初期化スタックを落とすため、
        // Autocast Amulet は whitelist へ明示登録して JEI 上でも Arcane Anvil 対象として拾わせる。
        tag(IRONS_IMBUE_WHITELIST).add(ItemRegistry.AUTOCAST_AMULET.get());

        var ironsUpgradeWhitelist = tag(IRONS_UPGRADE_WHITELIST);
        var malumMagicCapableWeaponTag = tag(MALUM_MAGIC_CAPABLE_WEAPON);
        var malumSoulHunterWeaponTag = tag(MALUM_SOUL_HUNTER_WEAPON);
        var malumSoulShatterCapableWeaponTag = tag(MALUM_SOUL_SHATTER_CAPABLE_WEAPON);
        var tomagicReversalWeaponTag = tag(TOMAGIC_REVERSAL_WEAPON);
        var transcendenceEnchantableTag = tag(TRANSCENDENCE_ENCHANTABLE);
        var wisdomEnchantableTag = tag(WISDOM_ENCHANTABLE);
        var plunderEnchantableTag = tag(PLUNDER_ENCHANTABLE);
        var synthesisEnchantableTag = tag(SYNTHESIS_ENCHANTABLE);
        var vanillaSwordEnchantableTag = tag(MINECRAFT_ENCHANTABLE_SWORD);
        var vanillaFireAspectEnchantableTag = tag(MINECRAFT_ENCHANTABLE_FIRE_ASPECT);
        var vanillaSharpWeaponEnchantableTag = tag(MINECRAFT_ENCHANTABLE_SHARP_WEAPON);
        var vanillaWeaponEnchantableTag = tag(MINECRAFT_ENCHANTABLE_WEAPON);
        ironsUpgradeWhitelist.add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.FOCUS_STAFFBOW.get()
        );
        // Focus Staffbow は StaffItem 継承に依存せず Staff 相当の主手エンチャ面を持たせたいので、
        // sword 系 tag と Malum 互換 tag、個別付与の Wisdom を明示追加する。
        malumMagicCapableWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        malumSoulHunterWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        malumSoulShatterCapableWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        tomagicReversalWeaponTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        wisdomEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        synthesisEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaSwordEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaFireAspectEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaSharpWeaponEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        vanillaWeaponEnchantableTag.add(ItemRegistry.FOCUS_STAFFBOW.get());
        // Iron's 側の upgrade 判定タグは実アイテム列挙なので、抽象基底クラス継承分を自動収集して取りこぼしを防ぐ。
        // Crystal Bladed Staff は 1.21.1 で継承階層が StaffItem 直下へ変わったため、明示列挙で維持する。
        for (var itemEntry : ItemRegistry.ITEMS.getEntries()) {
            var item = itemEntry.get();
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
                malumSoulShatterCapableWeaponTag.add(item);

                // 1.21.1 では Wisdom / Transcendence の適用可否が enchantment JSON 側の item tag で決まる。
                // 右クリック魔法武器は Java 側で両エンチャを許可しているため、tag 側も同じ面に揃える。
                if (item instanceof AbstractRightClickMagicWeaponItem) {
                    // 1.21.1 の Haunted / Animated は magic_capable_weapon タグ基準なので、
                    // 主手用魔法武器は tag と Java 側判定を同じ面に揃える。
                    malumMagicCapableWeaponTag.add(item);
                    transcendenceEnchantableTag.add(item);
                    wisdomEnchantableTag.add(item);
                    vanillaSwordEnchantableTag.add(item);
                    vanillaFireAspectEnchantableTag.add(item);
                    vanillaSharpWeaponEnchantableTag.add(item);
                    vanillaWeaponEnchantableTag.add(item);
                }
            }
        }
        tag(CURIOS_SPELLBOOK).add(
                ItemRegistry.ENDER_GRIMOIRE.get(),
                ItemRegistry.EXPLORERS_CODEX.get(),
                ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get(),
                ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get()
        );
        // 1.21.1 のバニラ防具 enchant は item tag 基準になったため、通常防具相当の分類へ入れる.
        tag(MINECRAFT_HEAD_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get()
        );
        tag(MINECRAFT_CHEST_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get()
        );
        tag(MINECRAFT_LEG_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get()
        );
        tag(MINECRAFT_FOOT_ARMOR).add(
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
        );
        tag(MINECRAFT_ENCHANTABLE_DURABILITY).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        tag(MINECRAFT_ENCHANTABLE_EQUIPPABLE).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
        );
        tag(MINECRAFT_ENCHANTABLE_VANISHING).add(
                ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get(),
                ItemRegistry.ELEMENTAL_BOW.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        // Elemental Bow は bow tag に参加させ、バニラ弓と同じ enchantment JSON 面を使う。
        tag(MINECRAFT_ENCHANTABLE_BOW).add(ItemRegistry.ELEMENTAL_BOW.get());
        // Elemental Bow は 1.21.1 でも Wisdom / Transcendence / Plunder を個別許可したいが、
        // spell_gun_enchantable に混ぜると Attunement まで通ってしまうため専用タグ側へ明示追加する。
        transcendenceEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        wisdomEnchantableTag.add(ItemRegistry.ELEMENTAL_BOW.get());
        plunderEnchantableTag.addTag(SPELL_GUN_ENCHANTABLE).add(ItemRegistry.ELEMENTAL_BOW.get());
        // 1.21.1 のバニラ enchantment JSON は Fortune / Silk Touch を mining_loot タグで判定する.
        tag(MINECRAFT_ENCHANTABLE_MINING_LOOT).add(ItemRegistry.PASTEL_STAFF.get());
        malumMagicCapableWeaponTag.add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get()
        );
        // Malum の soul_hunter_weapon は main hand 前提なので、
        // offhand 専用品を巻き込まず個別互換が必要な staff / shield だけ明示登録する。
        tag(MALUM_SOUL_HUNTER_WEAPON).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
        );
        // 1.21.1 の Spirit Plunder は soul_hunter_weapon ではなく soul_shatter_capable_weapon 経由で supported_items を見ている。
        // Java 側の許可だけでは definition 判定を通らないため、Malum 側 tag も同じ面へ揃える。
        tag(MALUM_SOUL_SHATTER_CAPABLE_WEAPON).add(
                ItemRegistry.PASTEL_STAFF.get(),
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.REFLECTCAST_SHIELD.get()
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
                ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get(),
                ItemRegistry.SILVER_SPELL_AMPLIFIER.get(),
                ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get(),
                ItemRegistry.PHOTON_SIPHON.get(),
                ItemRegistry.EXPLORERS_CANE.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        // spell gun 専用 enchant は offhand 補助具を巻き込まないように個別タグで分離する.
        tag(SPELL_GUN_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELLCASTER_GUN.get(),
                ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                ItemRegistry.GOLD_SPELLCASTER_GUN.get(),
                ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()
        );
        // 1.21.1 の enchantment JSON は supported_items tag を直接参照するため、
        // 飲用専用の Guzzle と共通 flask enchant 群を分離して誤適用を防ぐ。
        tag(DRINKABLE_FLASK_ENCHANTABLE).add(ItemRegistry.SPELLCASTERS_FLASK.get());
        // 錬金術師のフラスコは Large/Red/Glow/Transcendence のみを許可したいので、
        // spell container 系や Wisdom と混線しない専用タグで分離する。
        tag(ALCHEMISTS_FLASK_ENCHANTABLE).add(ItemRegistry.ALCHEMISTS_FLASK.get());
        tag(FLASK_ENCHANTABLE)
                .addTag(DRINKABLE_FLASK_ENCHANTABLE)
                .addTag(ALCHEMISTS_FLASK_ENCHANTABLE);
        tag(MANA_SHIELD_CHARM_ENCHANTABLE).add(ItemRegistry.MANA_SHIELD_CHARM.get());
        // Crystal Bladed Staff は Surge/Attunement などを避けつつ、個別指定の Wisdom/Transcendence のみ許可する。
        wisdomEnchantableTag.addTag(SPELL_GUN_ENCHANTABLE).add(
                ItemRegistry.CRYSTAL_BLADED_STAFF.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get(),
                ItemRegistry.ENCHANTRESS_HAT.get(),
                ItemRegistry.ENCHANTRESS_ROBE.get(),
                ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                ItemRegistry.ENCHANTRESS_BOOTS.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get(),
                ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
        );
        tag(OFFHAND_MAGIC_ENCHANTABLE).add(
                ItemRegistry.IRON_SPELL_AMPLIFIER.get(),
                ItemRegistry.COPPER_SPELL_AMPLIFIER.get(),
                ItemRegistry.GOLD_SPELL_AMPLIFIER.get(),
                ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get(),
                ItemRegistry.SILVER_SPELL_AMPLIFIER.get(),
                ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get(),
                ItemRegistry.PHOTON_SIPHON.get(),
                ItemRegistry.EXPLORERS_CANE.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        // 付呪魔女防具は 1.20.1 と同様に叡智のみを許可し、専用エンチャ系タグから外す.
        tag(OFFHAND_OR_ARMOR_MAGIC_ENCHANTABLE)
                .addTag(OFFHAND_MAGIC_ENCHANTABLE);
        tag(SPELL_CONTAINER_MAGIC_ENCHANTABLE)
                .addTag(OFFHAND_MAGIC_ENCHANTABLE)
                .addTag(SPELL_GUN_ENCHANTABLE);
        transcendenceEnchantableTag
                .addTag(SPELL_CONTAINER_MAGIC_ENCHANTABLE)
                .addTag(ALCHEMISTS_FLASK_ENCHANTABLE)
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
        tag(CURIOS_BACK).add(ItemRegistry.SPELLCASTER_QUIVER.get());
        tag(CURIOS_NECKLACE).add(
                ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get(),
                ItemRegistry.AUTOCAST_AMULET.get()
        );
        tag(CURIOS_HEAD).add(
                ItemRegistry.ASHEN_CIRCLET.get(),
                ItemRegistry.ENCHANTED_CIRCLET.get()
        );
        tag(CURIOS_CHARM).add(ItemRegistry.MANA_SHIELD_CHARM.get());
        tag(CREATE_CONTRAPTION_CONTROLLED).add(ItemRegistry.SPELL_DISPENSER.get());
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
        tag(TagRegistry.Items.SPELLCASTER_QUIVER_STORABLE).add(
                net.minecraft.world.item.Items.ARROW,
                net.minecraft.world.item.Items.SPECTRAL_ARROW,
                net.minecraft.world.item.Items.TIPPED_ARROW
        );
        tag(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE).add(
                ItemRegistry.ENCHANTED_CIRCLET.get()
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
